package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.exception.InternalServerException;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.verification.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ObjectMapper objectMapper;
    @Mock private TimeProvider timeProvider;
    @InjectMocks private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        service.init();
    }

    @Test
    void resendLimitRollsBackDailyCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.execute(any(), eq(List.of("sms:daily:01011112222")), any(), any())).thenReturn(1L);
        when(valueOperations.get("sms:resend:01011112222")).thenReturn("1");
        assertThatThrownBy(() -> service.createVerification("01011112222"))
                .isInstanceOf(ResendLimitExceededException.class);
        verify(valueOperations).decrement("sms:daily:01011112222");
    }

    @ParameterizedTest
    @CsvSource({
            "1,com.dduru.gildongmu.verification.exception.VerificationNotFoundException",
            "2,com.dduru.gildongmu.verification.exception.AlreadyVerifiedException",
            "3,com.dduru.gildongmu.verification.exception.InvalidVerificationCodeException",
            "4,com.dduru.gildongmu.verification.exception.VerificationAttemptsExceededException",
            "5,com.dduru.gildongmu.common.exception.InternalServerException"
    })
    void mapsAtomicVerificationResultToExistingException(long result, Class<? extends Throwable> exception) {
        when(redisTemplate.execute(any(), eq(List.of("sms:auth:01011112222")), eq("123456"), eq("5"), eq("600000")))
                .thenReturn(result);
        assertThatThrownBy(() -> service.verifyCode("01011112222", "123456")).isInstanceOf(exception);
    }

    @Test
    void nullScriptResultDoesNotVerify() {
        assertThatThrownBy(() -> service.verifyCode("01011112222", "123456"))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void successfulScriptResultVerifiesWithoutAnotherWrite() {
        when(redisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(0L);
        service.verifyCode("01011112222", "123456");
        verify(redisTemplate, never()).opsForValue();
    }
}
