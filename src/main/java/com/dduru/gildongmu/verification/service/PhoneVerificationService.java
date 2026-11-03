package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.jwt.JwtTokenProvider;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.verification.dto.response.VerificationSendResponse;
import com.dduru.gildongmu.verification.dto.response.VerificationVerifyResponse;
import com.dduru.gildongmu.verification.exception.DuplicatePhoneNumberException;
import com.dduru.gildongmu.verification.exception.SmsProviderException;
import com.dduru.gildongmu.verification.exception.SmsSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    @Value("${verification.admin-code:123456}")
    private String adminCode;

    private final SmsService smsService;
    private final VerificationCodeService verificationCodeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProfileRepository profileRepository;

    public VerificationSendResponse sendVerificationCode(String phoneNumber) {
        validatePhoneNumber(phoneNumber);

        VerificationCodeService.VerificationCreateResult result = 
                verificationCodeService.createVerification(phoneNumber);

        OffsetDateTime resendAvailableAt;
        try {
            sendVerificationSms(phoneNumber, result.code());
            resendAvailableAt = verificationCodeService.setResendLimit(phoneNumber);
        } catch (Exception e) {
            verificationCodeService.rollbackVerificationCreation(phoneNumber);
            log.error("Verification code 메서드 처리 실패", e);
            throw new SmsSendFailedException();
        }

        return VerificationSendResponse.builder()
                .expiresAt(result.expiresAt())
                .resendAvailableAt(resendAvailableAt)
                .build();
    }

    public VerificationVerifyResponse verifyCode(String phoneNumber, String code) {
        verificationCodeService.verifyCode(phoneNumber, code);
        String token = jwtTokenProvider.createVerificationToken(null, phoneNumber);
        return VerificationVerifyResponse.verified(token);
    }

    /**
     * Admin 테스트용 - SMS 발송 없이 고정 인증코드로 인증 생성.
     * 전화번호는 요청값 사용, 인증코드는 verification.admin-code 사용.
     */
    public VerificationSendResponse sendVerificationCodeAdmin(String phoneNumber) {
        validatePhoneNumber(phoneNumber);

        String code = (adminCode != null && !adminCode.isBlank()) ? adminCode : "123456";
        VerificationCodeService.VerificationCreateResult result =
                verificationCodeService.createVerificationWithAdminCode(phoneNumber, code);

        OffsetDateTime resendAvailableAt = verificationCodeService.setResendLimit(phoneNumber);

        return VerificationSendResponse.builder()
                .expiresAt(result.expiresAt())
                .resendAvailableAt(resendAvailableAt)
                .build();
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (profileRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicatePhoneNumberException();
        }
    }

    private void sendVerificationSms(String phoneNumber, String code) {
        String message = createVerificationMessage(code);
        
        try {
            smsService.sendSms(phoneNumber, message);
        } catch (Exception e) {
            log.error("SMS 발송 실패", e);
            throw new SmsProviderException();
        }
    }

    private String createVerificationMessage(String code) {
        return String.format("[뚜르] 인증번호는 [%s]입니다. 3분 내에 입력해주세요.", code);
    }
}
