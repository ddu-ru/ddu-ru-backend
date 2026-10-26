package com.dduru.gildongmu.chat.dto.ws;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * STOMP 클라이언트 전송 페이로드. SYSTEM과 POLL은 서버에서만 생성한다.
 * content 검증은 messageType 에 따라 달라서 서비스 계층의 전용 validator가 담당한다.
 */
@Schema(description = "STOMP 채팅 메시지 전송 요청. SYSTEM과 POLL은 서버에서만 생성하므로 클라이언트는 TEXT 또는 IMAGE만 전송합니다.")
public record ChatMessageSendRequest(
        @Schema(description = "전송할 메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE"})
        @NotNull(message = "메세지 타입은 필수입니다.")
        ChatMessageType messageType,

        @Schema(description = "TEXT는 메시지 본문, IMAGE는 이미지 URL", example = "안녕하세요!")
        String content
) {
}
