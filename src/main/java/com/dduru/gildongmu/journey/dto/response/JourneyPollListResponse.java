package com.dduru.gildongmu.journey.dto.response;

import java.util.List;

public record JourneyPollListResponse(List<JourneyPollSummaryResponse> polls, Long nextCursor, boolean hasNext, int size) {
    public static JourneyPollListResponse of(List<JourneyPollSummaryResponse> polls, boolean hasNext) {
        Long nextCursor = hasNext && !polls.isEmpty() ? polls.get(polls.size() - 1).pollId() : null;
        return new JourneyPollListResponse(polls, nextCursor, hasNext, polls.size());
    }
}
