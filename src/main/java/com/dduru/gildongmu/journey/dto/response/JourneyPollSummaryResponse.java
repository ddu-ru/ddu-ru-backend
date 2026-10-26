package com.dduru.gildongmu.journey.dto.response;

import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.domain.enums.JourneyPollStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record JourneyPollSummaryResponse(
        Long pollId,
        Long journeyId,
        String title,
        JourneyPollStatus status,
        LocalDateTime closesAt,
        boolean allowMultipleChoices,
        boolean anonymous,
        boolean allowOptionAddition,
        long voterCount,
        List<JourneyPollOptionResponse> options,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "현재 사용자의 선택. 공용 WebSocket 메시지에는 포함하지 않음")
        List<Long> selectedOptionIds
) {
    public static JourneyPollSummaryResponse from(JourneyPoll poll, LocalDateTime now, long voterCount,
                                                 List<JourneyPollOptionResponse> options, List<Long> selectedOptionIds) {
        return new JourneyPollSummaryResponse(poll.getId(), poll.getJourney().getId(), poll.getTitle(),
                poll.statusAt(now), poll.getClosesAt(), poll.isAllowMultipleChoices(), poll.isAnonymous(),
                poll.isAllowOptionAddition(), voterCount, options, selectedOptionIds);
    }
}
