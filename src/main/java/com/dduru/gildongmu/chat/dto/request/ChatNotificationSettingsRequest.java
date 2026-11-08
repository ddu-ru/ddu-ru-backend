package com.dduru.gildongmu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅방 알림 설정 변경 요청")
public record ChatNotificationSettingsRequest(
        @Schema(description = "채팅방 메시지 푸시 알림 수신 여부", example = "false")
        @NotNull(message = "enabled는 필수입니다.")
        Boolean enabled
) {
}
