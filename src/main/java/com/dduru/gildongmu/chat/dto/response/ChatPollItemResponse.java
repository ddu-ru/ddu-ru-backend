package com.dduru.gildongmu.chat.dto.response;

import com.dduru.gildongmu.journey.domain.enums.JourneyPollStatus;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;

import java.time.LocalDateTime;

public record ChatPollItemResponse(
        Long messageId,
        Long pollId,
        Long journeyId,
        String title,
        JourneyPollStatus status,
        long voterCount,
        LocalDateTime closesAt
) {
    public static ChatPollItemResponse from(Long messageId, JourneyPollSummaryResponse poll) {
        return new ChatPollItemResponse(
                messageId,
                poll.pollId(),
                poll.journeyId(),
                poll.title(),
                poll.status(),
                poll.voterCount(),
                poll.closesAt()
        );
    }
}
