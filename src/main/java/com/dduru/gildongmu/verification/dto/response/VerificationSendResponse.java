package com.dduru.gildongmu.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Builder
public record VerificationSendResponse(
        @Schema(description = "인증번호 만료 시각 (기존 KST 형식 유지)", example = "2026-06-29T12:39:56")
        LocalDateTime expiresAt,
        @Schema(description = "재발송 가능 시각 (KST 오프셋 포함)", example = "2026-06-29T12:37:56+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime resendAvailableAt
) {
}
