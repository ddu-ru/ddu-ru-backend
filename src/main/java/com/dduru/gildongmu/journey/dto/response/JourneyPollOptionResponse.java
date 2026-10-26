package com.dduru.gildongmu.journey.dto.response;

import com.dduru.gildongmu.journey.dto.query.JourneyPollOptionCountQueryResult;

public record JourneyPollOptionResponse(Long optionId, String content, long voteCount) {
    public static JourneyPollOptionResponse from(JourneyPollOptionCountQueryResult result) {
        return new JourneyPollOptionResponse(result.optionId(), result.content(), result.voteCount());
    }
}
