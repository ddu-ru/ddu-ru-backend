package com.dduru.gildongmu.chat.dto.response;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 목록에 표시되는 마지막 메시지 요약")
public record ChatRoomLastMessageResponse(
        @Schema(description = "마지막 메시지 ID", example = "120")
        Long messageId,

        @Schema(description = "마지막 메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "SYSTEM", "POLL"})
        ChatMessageType messageType,

        @Schema(description = "마지막 메시지 원문. TEXT는 텍스트, IMAGE는 이미지 URL, POLL은 투표 제목, SYSTEM은 시스템 payload 원문입니다.", example = "안녕하세요!")
        String content,

        @Schema(description = "발신자 ID. 시스템 메시지는 null일 수 있습니다.", example = "33", nullable = true)
        Long senderId,

        @Schema(description = "발신자 표시명. 시스템 메시지는 null일 수 있습니다.", example = "여행메이트", nullable = true)
        String senderNickname,

        @Schema(description = "마지막 메시지 생성 시각")
        LocalDateTime createdAt
) {
}
