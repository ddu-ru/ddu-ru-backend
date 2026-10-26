package com.dduru.gildongmu.journey.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record JourneyPollResponse(
        JourneyPollSummaryResponse poll,
        JourneyPollUserResponse author,
        LocalDateTime createdAt,
        @Schema(description = "비익명 투표만 투표자 공개. 익명 투표는 빈 배열") List<JourneyPollVoterResponse> voters
) {
}
