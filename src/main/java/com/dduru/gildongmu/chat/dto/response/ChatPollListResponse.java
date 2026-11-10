package com.dduru.gildongmu.chat.dto.response;

import java.util.List;

public record ChatPollListResponse(
        List<ChatPollItemResponse> polls,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    public static ChatPollListResponse of(List<ChatPollItemResponse> polls, boolean hasNext) {
        Long nextCursor = hasNext && !polls.isEmpty() ? polls.get(polls.size() - 1).messageId() : null;
        return new ChatPollListResponse(polls, nextCursor, hasNext, polls.size());
    }
}
