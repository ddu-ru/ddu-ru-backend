package com.dduru.gildongmu.chat.dto.ws;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.system.ChatSystemMessageFactory;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅방 토픽 구독자에게 브로드캐스트되는 메시지 페이로드.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Schema(description = "WebSocket 채팅방 토픽 메시지 payload")
public record ChatMessageBroadcastPayload(
        @Schema(description = "생성된 메시지 ID", example = "120")
        Long messageId,

        @Schema(description = "채팅방 ID", example = "10")
        Long roomId,

        @Schema(description = "연결 게시글 제목", example = "제주 애월 2박 3일")
        String postTitle,

        @Schema(description = "현재 승인 인원 수", example = "2")
        int recruitCount,

        @Schema(description = "모집 정원", example = "4")
        int recruitCapacity,

        @Schema(description = "메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "SYSTEM", "POLL"})
        ChatMessageType messageType,

        @Schema(description = "발신자 정보. SYSTEM 메시지는 null", nullable = true)
        ChatMessageSenderPayload sender,

        @Schema(description = "사용자 메시지 payload. TEXT와 IMAGE에서 사용", nullable = true)
        ChatUserMessagePayload userMessage,

        @Schema(description = "시스템 메시지 payload. SYSTEM에서 사용", nullable = true)
        ChatSystemMessagePayload systemMessage,

        @Schema(description = "메시지 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "POLL 카드. 사용자별 선택과 투표자 신원은 공용 메시지에 포함하지 않습니다.")
        JourneyPollSummaryResponse poll
) {
    public static ChatMessageBroadcastPayload ofUserMessage(ChatMessage message, Post post, User sender, ProfileImageResolver profileImageResolver) {
        return ChatMessageBroadcastPayload.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .postTitle(post.getTitle())
                .recruitCount(post.getRecruitCount())
                .recruitCapacity(post.getRecruitCapacity())
                .messageType(message.getMessageType())
                .sender(ChatMessageSenderPayload.from(sender, post.getUser().getId(), profileImageResolver))
                .userMessage(ChatUserMessagePayload.from(message.getMessageType(), message.getContent()))
                .systemMessage(null)
                .createdAt(message.getCreatedAt())
                .build();

    }

    public static ChatMessageBroadcastPayload ofSystemMessage(ChatMessage message, Post post, ChatSystemMessageFactory chatSystemMessageFactory) {
        return ChatMessageBroadcastPayload.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .postTitle(post.getTitle())
                .recruitCount(post.getRecruitCount())
                .recruitCapacity(post.getRecruitCapacity())
                .messageType(message.getMessageType())
                .sender(null)
                .userMessage(null)
                .systemMessage(chatSystemMessageFactory.deserialize(message.getContent()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatMessageBroadcastPayload ofPollMessage(ChatMessage message, Post post,
                                                            ProfileImageResolver profileImageResolver,
                                                            JourneyPollSummaryResponse poll) {
        return ChatMessageBroadcastPayload.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .postTitle(post.getTitle())
                .recruitCount(post.getRecruitCount())
                .recruitCapacity(post.getRecruitCapacity())
                .messageType(ChatMessageType.POLL)
                .sender(ChatMessageSenderPayload.from(message.getSender(), post.getUser().getId(), profileImageResolver))
                .createdAt(message.getCreatedAt())
                .poll(poll)
                .build();
    }

}
