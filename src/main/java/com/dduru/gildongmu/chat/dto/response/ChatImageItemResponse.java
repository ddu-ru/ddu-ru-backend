package com.dduru.gildongmu.chat.dto.response;

import com.dduru.gildongmu.chat.domain.ChatMessage;

import java.time.LocalDateTime;

public record ChatImageItemResponse(
        Long messageId,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static ChatImageItemResponse from(ChatMessage message) {
        return new ChatImageItemResponse(message.getId(), message.getContent(), message.getCreatedAt());
    }
}
