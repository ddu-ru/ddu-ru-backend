package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.exception.InternalServerException;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.time.KoreaTime;
import com.dduru.gildongmu.verification.dto.VerificationData;
import com.dduru.gildongmu.verification.exception.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;
    
    private static final String REDIS_KEY_PREFIX = "sms:auth:";
    private static final String DAILY_LIMIT_KEY_PREFIX = "sms:daily:";
    private static final String RESEND_LIMIT_KEY_PREFIX = "sms:resend:";
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 3;
    private static final int VERIFIED_EXPIRATION_MINUTES = 10;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int DAILY_SMS_LIMIT = 50;
    private static final int RESEND_LIMIT_MINUTES = 1;
    private static final int DAILY_LIMIT_TTL_SECONDS = 24 * 60 * 60;
    private static final String STATUS_PENDING = "PENDING";
    private static final SecureRandom random = new SecureRandom();
    
    private final DefaultRedisScript<List> sendLimitsScript = script("check-send-limits.lua", List.class);
    private final DefaultRedisScript<Long> resendLimitScript = script("set-resend-limit.lua", Long.class);
    private final DefaultRedisScript<Long> verifyCodeScript = script("verify-code.lua", Long.class);
    private final DefaultRedisScript<Long> rollbackSendScript = script("rollback-send.lua", Long.class);
    private final DefaultRedisScript<Long> saveCodeScript = script("save-code.lua", Long.class);

    private static <T> DefaultRedisScript<T> script(String name, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/verification/" + name));
        script.setResultType(resultType);
        return script;
    }

    public VerificationCreateResult createVerification(String phoneNumber) {
        return createVerification(phoneNumber, false, null);
    }

    /**
     * Admin 테스트용 - 고정 인증코드 사용
     */
    public VerificationCreateResult createVerificationWithAdminCode(String phoneNumber, String adminCode) {
        return createVerification(phoneNumber, true, adminCode);
    }

    private VerificationCreateResult createVerification(String phoneNumber, boolean useAdminCode, String adminCode) {
        checkAndIncrementSendLimits(phoneNumber);

        String code = useAdminCode ? adminCode : generateCode();
        VerificationData data = createVerificationData(phoneNumber, code);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                timeProvider.instant().truncatedTo(ChronoUnit.MILLIS), KoreaTime.ZONE_ID)
                .plusMinutes(EXPIRATION_MINUTES);
        saveVerificationData(phoneNumber, data, expiresAt);
        
        return new VerificationCreateResult(code, expiresAt);
    }

    public void verifyCode(String phoneNumber, String inputCode) {
        Long result = redisTemplate.execute(verifyCodeScript, List.of(REDIS_KEY_PREFIX + phoneNumber),
                inputCode, String.valueOf(MAX_VERIFICATION_ATTEMPTS),
                String.valueOf(Duration.ofMinutes(VERIFIED_EXPIRATION_MINUTES).toMillis()));
        if (result == null) throw new InternalServerException();
        switch (result.intValue()) {
            case 0 -> { }
            case 1 -> throw new VerificationNotFoundException();
            case 2 -> throw new AlreadyVerifiedException();
            case 3 -> throw new InvalidVerificationCodeException();
            case 4 -> throw new VerificationAttemptsExceededException();
            default -> throw new InternalServerException();
        }
    }

    public void rollbackVerificationCreation(String phoneNumber) {
        redisTemplate.execute(rollbackSendScript,
                List.of(REDIS_KEY_PREFIX + phoneNumber, DAILY_LIMIT_KEY_PREFIX + phoneNumber));
    }

    public OffsetDateTime setResendLimit(String phoneNumber) {
        Long expiresAtMillis = redisTemplate.execute(resendLimitScript,
                List.of(RESEND_LIMIT_KEY_PREFIX + phoneNumber),
                String.valueOf(Duration.ofMinutes(RESEND_LIMIT_MINUTES).toMillis()));
        return toResendAvailableAt(expiresAtMillis);
    }

    private VerificationData createVerificationData(String phoneNumber, String code) {
        return VerificationData.builder()
                .code(code)
                .phone(phoneNumber)
                .count(0)
                .status(STATUS_PENDING)
                .build();
    }

    private void saveVerificationData(String phoneNumber, VerificationData data, LocalDateTime expiresAt) {
        String redisKey = REDIS_KEY_PREFIX + phoneNumber;
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            long expiresAtMillis = expiresAt.atZone(KoreaTime.ZONE_ID).toInstant().toEpochMilli();
            Long result = redisTemplate.execute(saveCodeScript, List.of(redisKey),
                    jsonData, String.valueOf(expiresAtMillis));
            if (result == null || result != 1) throw new InternalServerException();
        } catch (JsonProcessingException e) {
            log.error("인증 데이터 저장 실패", e);
            throw new InternalServerException();
        }
    }

    private String generateCode() {
        int min = (int) Math.pow(10, CODE_LENGTH - 1);
        int max = (int) Math.pow(10, CODE_LENGTH) - 1;
        int code = random.nextInt(max - min + 1) + min;
        return String.valueOf(code);
    }

    private void checkAndIncrementSendLimits(String phoneNumber) {
        List<?> result = redisTemplate.execute(sendLimitsScript,
                List.of(DAILY_LIMIT_KEY_PREFIX + phoneNumber, RESEND_LIMIT_KEY_PREFIX + phoneNumber),
                String.valueOf(DAILY_SMS_LIMIT), String.valueOf(DAILY_LIMIT_TTL_SECONDS));
        if (result == null || result.size() != 2 || !(result.get(0) instanceof Long status)
                || !(result.get(1) instanceof Long)) {
            throw new InternalServerException();
        }
        switch (status.intValue()) {
            case 0 -> { }
            case 1 -> throw new DailySmsLimitExceededException(toRetryAfterSeconds((Long) result.get(1)));
            case 2 -> throw new ResendLimitExceededException(toRetryAfterSeconds((Long) result.get(1)));
            default -> throw new InternalServerException();
        }
    }

    private OffsetDateTime toResendAvailableAt(Long expiresAtMillis) {
        if (expiresAtMillis == null || expiresAtMillis <= 0) throw new InternalServerException();
        return Instant.ofEpochMilli(expiresAtMillis).atZone(KoreaTime.ZONE_ID).toOffsetDateTime();
    }

    private long toRetryAfterSeconds(Long expiresAtMillis) {
        if (expiresAtMillis == null || expiresAtMillis <= 0) throw new InternalServerException();
        Duration remaining = Duration.between(timeProvider.instant(), Instant.ofEpochMilli(expiresAtMillis));
        if (remaining.isNegative() || remaining.isZero()) return 0;
        return remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
    }

    public record VerificationCreateResult(String code, LocalDateTime expiresAt) {
    }
}
