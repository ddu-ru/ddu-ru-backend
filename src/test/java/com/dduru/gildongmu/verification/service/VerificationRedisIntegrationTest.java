package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.exception.InternalServerException;
import com.dduru.gildongmu.common.time.KoreaTime;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.verification.dto.VerificationData;
import com.dduru.gildongmu.verification.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("휴대폰 인증 Redis 통합 테스트")
class VerificationRedisIntegrationTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, String> redis;
    private final ObjectMapper mapper = new ObjectMapper();
    private VerificationCodeService service;
    private static final String PHONE = "01011112222";
    private static final String AUTH = "sms:auth:" + PHONE;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new RedisTemplate<>();
        redis.setConnectionFactory(connectionFactory);
        redis.setKeySerializer(new StringRedisSerializer());
        redis.setValueSerializer(new StringRedisSerializer());
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redis.delete(redis.keys("sms:*"));
        service = new VerificationCodeService(redis, mapper, new TimeProvider(Clock.system(KoreaTime.ZONE_ID)));
    }

    @Test
    @DisplayName("발급 응답의 만료 시각과 Redis에 저장된 만료 시각이 일치한다")
    void matchesExpiry() {
        var result = service.createVerificationWithAdminCode(PHONE, "123456");
        assertThat(expiration(AUTH)).isEqualTo(result.expiresAt().atZone(KoreaTime.ZONE_ID).toInstant().toEpochMilli());
        assertThat(redis.getExpire(AUTH, TimeUnit.MILLISECONDS)).isBetween(175_000L, 180_000L);
    }

    @Test
    @DisplayName("인증번호를 틀려도 최초 만료 시각을 유지한다")
    void preservesExpiry() throws Exception {
        createCode();
        redis.expire(AUTH, Duration.ofSeconds(20));
        long originalExpiry = expiration(AUTH);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(expiration(AUTH)).isEqualTo(originalExpiry);
        assertThat(data().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("만료된 인증번호는 검증을 거절하고 다시 생성하지 않는다")
    void rejectsExpiredCode() {
        createCode();
        redis.expire(AUTH, Duration.ZERO);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "123456")).isInstanceOf(VerificationNotFoundException.class);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(VerificationNotFoundException.class);
        assertThat(redis.hasKey(AUTH)).isFalse();
    }

    @Test
    @DisplayName("만료 시간이 없는 인증번호는 내부 오류로 처리한다")
    void rejectsPersistentCode() {
        createCode();
        redis.persist(AUTH);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "123456")).isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("동시 오입력 횟수를 누락 없이 반영하고 만료를 유지하며 한도 초과 시 삭제한다")
    void countsConcurrentFailures() throws Exception {
        createCode();
        long originalExpiry = expiration(AUTH);
        var results = concurrently(5, "000000");
        assertThat(results).allMatch(result -> result.equals("InvalidVerificationCodeException"));
        assertThat(data().count()).isEqualTo(5);
        assertThat(expiration(AUTH)).isEqualTo(originalExpiry);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(VerificationAttemptsExceededException.class);
        assertThat(redis.hasKey(AUTH)).isFalse();
    }

    @Test
    @DisplayName("동시 정상 인증은 한 번만 성공하고 인증 상태를 10분간 유지한다")
    void verifiesOnce() throws Exception {
        createCode();
        assertThat(concurrently(2, "123456")).containsExactlyInAnyOrder("verified", "AlreadyVerifiedException");
        assertThat(data().status()).isEqualTo("VERIFIED");
        assertThat(redis.getExpire(AUTH, TimeUnit.MILLISECONDS)).isBetween(595_000L, 600_000L);
    }

    @Test
    @DisplayName("재발송 성공 시 해제 시각을 반환하고 거절 시 실제 남은 초와 발송 횟수를 유지한다")
    void returnsResendWait() {
        createCode();
        long dailyExpiry = expiration("sms:daily:" + PHONE);
        var availableAt = service.setResendLimit(PHONE);
        assertThat(availableAt.getOffset()).isEqualTo(java.time.ZoneOffset.ofHours(9));
        assertThat(availableAt.toInstant().toEpochMilli()).isEqualTo(expiration("sms:resend:" + PHONE));
        assertThat(redis.getExpire("sms:resend:" + PHONE, TimeUnit.MILLISECONDS)).isBetween(55_000L, 60_000L);
        assertThatThrownBy(this::createCode).isInstanceOfSatisfying(ResendLimitExceededException.class,
                exception -> assertThat(exception.getRetryAfterSeconds())
                        .isBetween((long) Math.ceil(redis.getExpire("sms:resend:" + PHONE, TimeUnit.MILLISECONDS) / 1000.0), 60L));
        assertThat(redis.opsForValue().get("sms:daily:" + PHONE)).isEqualTo("1");
        assertThat(expiration("sms:daily:" + PHONE)).isEqualTo(dailyExpiry);
        redis.expire("sms:resend:" + PHONE, Duration.ZERO);
        createCode();
        assertThat(redis.opsForValue().get("sms:daily:" + PHONE)).isEqualTo("2");
        assertThat(expiration("sms:daily:" + PHONE)).isEqualTo(dailyExpiry);
    }

    @Test
    @DisplayName("발송 한도 초과 시 실제 남은 초를 반환하고 만료 후 재발급을 허용한다")
    void returnsDailyWait() {
        String dailyKey = "sms:daily:" + PHONE;
        redis.opsForValue().set(dailyKey, "50", Duration.ofHours(2));
        long originalExpiry = expiration(dailyKey);
        assertThatThrownBy(this::createCode).isInstanceOfSatisfying(DailySmsLimitExceededException.class,
                exception -> assertThat(exception.getRetryAfterSeconds())
                        .isBetween((long) Math.ceil(redis.getExpire(dailyKey, TimeUnit.MILLISECONDS) / 1000.0), 7200L));
        assertThat(redis.opsForValue().get(dailyKey)).isEqualTo("50");
        assertThat(expiration(dailyKey)).isEqualTo(originalExpiry);
        assertThat(redis.hasKey(AUTH)).isFalse();
        redis.expire(dailyKey, Duration.ZERO);
        createCode();
        assertThat(redis.opsForValue().get(dailyKey)).isEqualTo("1");
        assertThat(redis.getExpire(dailyKey, TimeUnit.MILLISECONDS)).isBetween(86_395_000L, 86_400_000L);
    }

    @Test
    @DisplayName("재시도 대기 시간은 남은 초를 올림하고 이미 지난 시각은 0초로 반환한다")
    void roundsRetryWait() {
        createCode();
        long deadline = service.setResendLimit(PHONE).toInstant().toEpochMilli();
        long[][] cases = {{45_001, 46}, {1, 1}, {0, 0}, {-1, 0}};
        for (long[] testCase : cases) {
            Clock clock = Clock.fixed(java.time.Instant.ofEpochMilli(deadline - testCase[0]), KoreaTime.ZONE_ID);
            service = new VerificationCodeService(redis, mapper, new TimeProvider(clock));
            assertThatThrownBy(this::createCode).isInstanceOfSatisfying(ResendLimitExceededException.class,
                    exception -> assertThat(exception.getRetryAfterSeconds()).isEqualTo(testCase[1]));
        }
    }

    @Test
    @DisplayName("발송 카운터는 최초 생성부터 24시간을 유지하고 롤백 후에도 만료를 연장하지 않는다")
    void preservesDailyWindow() {
        createCode();
        String dailyKey = "sms:daily:" + PHONE;
        long originalExpiry = expiration(dailyKey);
        assertThat(redis.getExpire(dailyKey, TimeUnit.MILLISECONDS)).isBetween(86_395_000L, 86_400_000L);
        service.rollbackVerificationCreation(PHONE);
        assertThat(redis.opsForValue().get(dailyKey)).isEqualTo("0");
        assertThat(redis.hasKey(AUTH)).isFalse();
        createCode();
        assertThat(expiration(dailyKey)).isEqualTo(originalExpiry);
    }

    @Test
    @DisplayName("만료된 카운터를 롤백해도 무기한 음수 카운터를 생성하지 않는다")
    void skipsExpiredRollback() {
        createCode();
        redis.expire("sms:daily:" + PHONE, Duration.ZERO);
        service.rollbackVerificationCreation(PHONE);
        assertThat(redis.hasKey("sms:daily:" + PHONE)).isFalse();
        assertThat(redis.hasKey(AUTH)).isFalse();
        createCode();
        assertThat(redis.opsForValue().get("sms:daily:" + PHONE)).isEqualTo("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms:daily:", "sms:resend:"})
    @DisplayName("만료 시간이 없는 발송 제한은 횟수 변경과 인증번호 생성 없이 내부 오류로 처리한다")
    void rejectsPersistentLimits(String prefix) {
        redis.opsForValue().set(prefix + PHONE, "1");
        assertThatThrownBy(this::createCode).isInstanceOf(InternalServerException.class);
        assertThat(redis.opsForValue().get(prefix + PHONE)).isEqualTo("1");
        assertThat(redis.hasKey(AUTH)).isFalse();
    }

    private void createCode() {
        service.createVerificationWithAdminCode(PHONE, "123456");
    }

    private VerificationData data() throws Exception {
        return mapper.readValue(redis.opsForValue().get(AUTH), VerificationData.class);
    }

    private long expiration(String key) {
        return redis.execute(new DefaultRedisScript<>("return redis.call('PEXPIRETIME', KEYS[1])", Long.class), List.of(key));
    }

    private List<String> concurrently(int count, String code) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try {
            var futures = IntStream.range(0, count).mapToObj(ignored -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    service.verifyCode(PHONE, code);
                    return "verified";
                } catch (com.dduru.gildongmu.common.exception.BusinessException exception) {
                    return exception.getClass().getSimpleName();
                }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new java.util.ArrayList<String>();
            for (var future : futures) results.add(future.get(10, TimeUnit.SECONDS));
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
