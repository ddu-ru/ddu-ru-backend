package com.dduru.gildongmu.chat.dto.response;

import java.util.List;

public record ChatImageListResponse(
        List<ChatImageItemResponse> images,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    public static ChatImageListResponse of(List<ChatImageItemResponse> images, boolean hasNext) {
        Long nextCursor = hasNext && !images.isEmpty() ? images.get(images.size() - 1).messageId() : null;
        return new ChatImageListResponse(images, nextCursor, hasNext, images.size());
    }
}
