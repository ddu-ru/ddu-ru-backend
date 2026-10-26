package com.dduru.gildongmu.journey.dto.request;

import com.dduru.gildongmu.journey.domain.JourneyPoll;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JourneyPollVoteRequest(
        @NotEmpty @Size(max = JourneyPoll.MAX_OPTIONS)
        @Schema(description = "내 선택 전체. 단일 선택은 1개, 복수 선택은 1~10개. 중복 ID 불가")
        List<@NotNull @Positive Long> optionIds
) {
}
