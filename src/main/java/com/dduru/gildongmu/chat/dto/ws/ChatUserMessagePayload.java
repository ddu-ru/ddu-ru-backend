package com.dduru.gildongmu.chat.dto.ws;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.exception.ChatSystemMessageSendAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatPollMessageSendAccessDeniedException;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "WebSocket 사용자 메시지 payload. 메시지 타입에 따라 text 또는 imageUrl 중 하나만 사용합니다.")
public record ChatUserMessagePayload(
        @Schema(description = "TEXT 메시지 본문", example = "안녕하세요!", nullable = true)
        String text,

        @Schema(description = "IMAGE 메시지 이미지 URL", example = "https://example.com/images/chat-image.png", nullable = true)
        String imageUrl
) {

    public static ChatUserMessagePayload from(ChatMessageType messageType, String content) {
        return switch (messageType) {
            case TEXT -> new ChatUserMessagePayload(content, null);
            case IMAGE -> new ChatUserMessagePayload(null, content);
            case SYSTEM -> throw new ChatSystemMessageSendAccessDeniedException();
            case POLL -> throw new ChatPollMessageSendAccessDeniedException();
        };
    }
}
