package com.dduru.gildongmu.verification.controller;

import com.dduru.gildongmu.common.exception.GlobalExceptionHandler;
import com.dduru.gildongmu.verification.dto.response.VerificationSendResponse;
import com.dduru.gildongmu.verification.exception.DailySmsLimitExceededException;
import com.dduru.gildongmu.verification.exception.InvalidVerificationCodeException;
import com.dduru.gildongmu.verification.exception.ResendLimitExceededException;
import com.dduru.gildongmu.verification.service.PhoneVerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@JsonTest
@ContextConfiguration(classes = VerificationResponseTest.JsonConfig.class)
@DisplayName("휴대폰 인증 API 응답 테스트")
class VerificationResponseTest {
    @Configuration(proxyBeanMethods = false)
    static class JsonConfig { }

    @Autowired private ObjectMapper mapper;
    private PhoneVerificationService service;
    private MockMvc mvc;
    private static final OffsetDateTime RESEND_AT = OffsetDateTime.parse("2026-11-02T15:01:00+09:00");
    private static final String SEND_BODY = "{\"phoneNumber\":\"01011112222\"}";

    @BeforeEach
    void setUp() {
        service = mock(PhoneVerificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PhoneVerificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    @DisplayName("발송 응답은 기존 만료 시각 형식을 유지하고 재발송 시각에 시간대를 포함한다")
    void preservesTimeFormat() throws Exception {
        var response = new VerificationSendResponse(LocalDateTime.of(2026, 11, 2, 15, 3), RESEND_AT);
        when(service.sendVerificationCode(anyString())).thenReturn(response);
        when(service.sendVerificationCodeAdmin(anyString())).thenReturn(response);
        for (String path : new String[]{"/api/v1/verifications", "/api/v1/verifications/admin"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(SEND_BODY))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                    .andExpect(jsonPath("$.data.expiresAt").value("2026-11-02T15:03:00"))
                    .andExpect(jsonPath("$.data.resendAvailableAt").value("2026-11-02T15:01:00+09:00"));
        }
    }

    @Test
    @DisplayName("재발송 제한 응답은 기존 오류 본문을 유지하고 Retry-After 헤더로 대기 시간을 반환한다")
    void returnsResendError() throws Exception {
        when(service.sendVerificationCode(anyString())).thenThrow(new ResendLimitExceededException(45L));
        mvc.perform(post("/api/v1/verifications").contentType(MediaType.APPLICATION_JSON).content(SEND_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.errorCode").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.data.message").value("재발송 제한 시간이 지나지 않았습니다."))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "45"))
                .andExpect(content().json("""
                        {"status":429,"data":{"errorCode":"TOO_MANY_REQUESTS","message":"재발송 제한 시간이 지나지 않았습니다."}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("발송 한도 초과 응답은 기존 오류 본문과 Retry-After 헤더를 반환한다")
    void returnsDailyError() throws Exception {
        when(service.sendVerificationCode(anyString())).thenThrow(new DailySmsLimitExceededException(7200L));
        mvc.perform(post("/api/v1/verifications").contentType(MediaType.APPLICATION_JSON).content(SEND_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.data.errorCode").value("DAILY_SMS_LIMIT_EXCEEDED"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "7200"))
                .andExpect(content().json("""
                        {"status":429,"data":{"errorCode":"DAILY_SMS_LIMIT_EXCEEDED","message":"일일 SMS 발송 한도를 초과했습니다."}}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("인증번호 불일치 오류에는 Retry-After 헤더를 포함하지 않는다")
    void omitsRetryHeader() throws Exception {
        when(service.verifyCode(anyString(), anyString())).thenThrow(new InvalidVerificationCodeException());
        mvc.perform(patch("/api/v1/verifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"01011112222\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("INVALID_AUTH_CODE"))
                .andExpect(jsonPath("$.data.retryAt").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER));
    }
}
