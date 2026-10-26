package com.dduru.gildongmu.journey.dto.response;

import com.dduru.gildongmu.journey.domain.JourneyPollVote;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;

public record JourneyPollVoterResponse(Long optionId, JourneyPollUserResponse voter) {
    public static JourneyPollVoterResponse from(JourneyPollVote vote, ProfileImageResolver resolver) {
        return new JourneyPollVoterResponse(vote.getOption().getId(), JourneyPollUserResponse.from(vote.getUser(), resolver));
    }
}
