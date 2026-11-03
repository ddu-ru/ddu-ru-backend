package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.jwt.JwtTokenProvider;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.verification.dto.response.VerificationVerifyResponse;
import com.dduru.gildongmu.verification.exception.DuplicatePhoneNumberException;
import com.dduru.gildongmu.verification.exception.SmsSendFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("휴대폰 인증 서비스 테스트")
class PhoneVerificationServiceTest {

    @Mock
    private SmsService smsService;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private PhoneVerificationService phoneVerificationService;

    @Test
    @DisplayName("이미 가입된 전화번호로 발송을 요청하면 거절한다")
    void rejectsRegisteredPhone() {
        // given
        when(profileRepository.existsByPhoneNumber("01012345678")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode("01012345678"))
                .isInstanceOf(DuplicatePhoneNumberException.class);

        verify(verificationCodeService, never()).createVerification(anyString());
    }

    @Test
    @DisplayName("SMS 발송이 실패하면 인증 생성과 발송 횟수를 롤백한다")
    void rollsBackFailedSms() {
        // given
        when(profileRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        VerificationCodeService.VerificationCreateResult result =
                new VerificationCodeService.VerificationCreateResult("999999", java.time.LocalDateTime.now().plusMinutes(3));
        when(verificationCodeService.createVerification("01022223333")).thenReturn(result);
        doThrow(new RuntimeException("sms fail")).when(smsService).sendSms(anyString(), anyString());

        // when & then
        assertThatThrownBy(() -> phoneVerificationService.sendVerificationCode("01022223333"))
                .isInstanceOf(SmsSendFailedException.class);

        verify(verificationCodeService).rollbackVerificationCreation("01022223333");
        verify(verificationCodeService, never()).setResendLimit(anyString());
    }

    @Test
    @DisplayName("SMS 발송 성공 후 인증 만료 시각과 재발송 가능 시각을 반환한다")
    void returnsSendTimes() {
        var expiresAt = java.time.LocalDateTime.of(2026, 11, 2, 15, 3);
        var resendAt = java.time.OffsetDateTime.parse("2026-11-02T15:01:00+09:00");
        when(verificationCodeService.createVerification("01011112222"))
                .thenReturn(new VerificationCodeService.VerificationCreateResult("123456", expiresAt));
        when(verificationCodeService.setResendLimit("01011112222")).thenReturn(resendAt);
        var response = phoneVerificationService.sendVerificationCode("01011112222");
        org.assertj.core.api.Assertions.assertThat(response.expiresAt()).isEqualTo(expiresAt);
        org.assertj.core.api.Assertions.assertThat(response.resendAvailableAt()).isEqualTo(resendAt);
        var order = inOrder(smsService, verificationCodeService);
        order.verify(verificationCodeService).createVerification("01011112222");
        order.verify(smsService).sendSms(eq("01011112222"), anyString());
        order.verify(verificationCodeService).setResendLimit("01011112222");
    }

    @Test
    @DisplayName("관리자 발송은 SMS 없이 동일한 시간 정보를 반환한다")
    void returnsAdminTimes() {
        var expiresAt = java.time.LocalDateTime.of(2026, 11, 2, 15, 3);
        var resendAt = java.time.OffsetDateTime.parse("2026-11-02T15:01:00+09:00");
        when(verificationCodeService.createVerificationWithAdminCode("01011112222", "123456"))
                .thenReturn(new VerificationCodeService.VerificationCreateResult("123456", expiresAt));
        when(verificationCodeService.setResendLimit("01011112222")).thenReturn(resendAt);
        var response = phoneVerificationService.sendVerificationCodeAdmin("01011112222");
        org.assertj.core.api.Assertions.assertThat(response.expiresAt()).isEqualTo(expiresAt);
        org.assertj.core.api.Assertions.assertThat(response.resendAvailableAt()).isEqualTo(resendAt);
        verifyNoInteractions(smsService);
    }

    @Test
    @DisplayName("인증번호 검증에 성공하면 인증 토큰을 반환한다")
    void returnsToken() {
        // given
        when(jwtTokenProvider.createVerificationToken(null, "01033334444")).thenReturn("token");

        // when
        VerificationVerifyResponse response = phoneVerificationService.verifyCode("01033334444", "123456");

        // then
        verify(verificationCodeService).verifyCode("01033334444", "123456");
        verify(jwtTokenProvider).createVerificationToken(null, "01033334444");
        org.assertj.core.api.Assertions.assertThat(response.token()).isEqualTo("token");
    }
}
