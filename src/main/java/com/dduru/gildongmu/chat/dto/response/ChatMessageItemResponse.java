package com.dduru.gildongmu.chat.dto.response;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.dto.ws.ChatSystemMessagePayload;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "채팅 메시지 항목. messageType에 따라 content, images, systemMessage, poll 중 하나를 사용합니다.")
public record ChatMessageItemResponse(
        @Schema(description = "메시지 ID", example = "120")
        Long messageId,

        @Schema(description = "메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "SYSTEM", "POLL"})
        ChatMessageType messageType,

        @Schema(description = "발신자 정보. SYSTEM 메시지는 null", nullable = true)
        ChatMessageSenderResponse sender,

        @Schema(description = "현재 사용자가 보낸 메시지인지 여부. SYSTEM 메시지는 false", example = "true")
        boolean isMine,

        @Schema(description = "TEXT 메시지 본문. 다른 타입은 null", example = "안녕하세요!", nullable = true)
        String content,

        @Schema(description = "IMAGE 메시지 이미지 목록. 다른 타입은 빈 배열")
        List<ChatMessageImageResponse> images,

        @Schema(description = "SYSTEM 메시지 표시 정보. 다른 타입은 null", nullable = true)
        ChatSystemMessageResponse systemMessage,

        @Schema(description = "이 메시지를 아직 읽지 않은 멤버 수. SYSTEM 메시지는 null", example = "2", nullable = true)
        Integer unreadCount,

        @Schema(description = "메시지 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "POLL 메시지 카드. 본인 선택과 최신 집계 포함", nullable = true)
        JourneyPollSummaryResponse poll
) {
    public static ChatMessageItemResponse forText(
            ChatMessage message,
            Long hostUserId,
            Long currentUserId,
            Integer unreadCount
    ) {
        return new ChatMessageItemResponse(
                message.getId(),
                message.getMessageType(),
                ChatMessageSenderResponse.from(message.getSender(), hostUserId),
                isMine(message.getSender(), currentUserId),
                message.getContent(),
                List.of(),
                null,
                unreadCount,
                message.getCreatedAt(),
                null
        );
    }

    public static ChatMessageItemResponse forImage(
            ChatMessage message,
            Long hostUserId,
            Long currentUserId,
            Integer unreadCount
    ) {
        return new ChatMessageItemResponse(
                message.getId(),
                message.getMessageType(),
                ChatMessageSenderResponse.from(message.getSender(), hostUserId),
                isMine(message.getSender(), currentUserId),
                null,
                List.of(ChatMessageImageResponse.from(message.getContent())),
                null,
                unreadCount,
                message.getCreatedAt(),
                null
        );
    }

    public static ChatMessageItemResponse forSystem(
            ChatMessage message,
            ChatSystemMessagePayload payload,
            Map<Long, User> systemUsers
    ) {
        return new ChatMessageItemResponse(
                message.getId(),
                message.getMessageType(),
                null,
                false,
                null,
                List.of(),
                ChatSystemMessageResponse.from(payload, systemUsers),
                null,
                message.getCreatedAt(),
                null
        );
    }

    public static ChatMessageItemResponse forPoll(ChatMessage message, Long hostUserId, Long currentUserId,
                                                   Integer unreadCount, JourneyPollSummaryResponse poll) {
        return new ChatMessageItemResponse(message.getId(), message.getMessageType(),
                ChatMessageSenderResponse.from(message.getSender(), hostUserId), isMine(message.getSender(), currentUserId),
                null, List.of(), null, unreadCount, message.getCreatedAt(), poll);
    }

    private static boolean isMine(User sender, Long currentUserId) {
        return sender != null && sender.getId().equals(currentUserId);
    }
}
