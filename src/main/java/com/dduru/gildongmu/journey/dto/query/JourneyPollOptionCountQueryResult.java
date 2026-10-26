package com.dduru.gildongmu.journey.dto.query;

public record JourneyPollOptionCountQueryResult(Long pollId, Long optionId, String content, long voteCount) {
}
