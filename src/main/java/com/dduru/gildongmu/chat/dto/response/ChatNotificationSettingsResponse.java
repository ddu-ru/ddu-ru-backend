package com.dduru.gildongmu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자의 채팅방 알림 설정")
public record ChatNotificationSettingsResponse(
        @Schema(description = "채팅방 ID", example = "1")
        Long chatRoomId,

        @Schema(description = "채팅방별 알림 수신 설정값. true여도 전체 알림이 OFF이면 푸시 미발송", example = "false")
        boolean enabled
) {
}
