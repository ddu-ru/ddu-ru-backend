package com.dduru.gildongmu.chat.dto.ws;

public record ChatPollUpdatedPayload(String eventType, Long roomId, Long journeyId, Long pollId) {
    public static ChatPollUpdatedPayload of(Long roomId, Long journeyId, Long pollId) {
        return new ChatPollUpdatedPayload("POLL_UPDATED", roomId, journeyId, pollId);
    }
}
