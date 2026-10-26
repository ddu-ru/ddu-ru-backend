package com.dduru.gildongmu.journey.dto.request;

import com.dduru.gildongmu.journey.domain.JourneyPoll;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record JourneyPollCreateRequest(
        @NotBlank @Schema(description = "투표 제목. 최대 100자") String title,
        @NotNull @Size(min = JourneyPoll.MIN_OPTIONS, max = JourneyPoll.MAX_OPTIONS)
        @Schema(description = "중복 없는 선택지 2~10개. 각 최대 100자") List<@NotBlank String> options,
        boolean allowMultipleChoices,
        boolean anonymous,
        boolean allowOptionAddition,
        @NotNull @Schema(description = "종료 시각. 서비스 시간대(Asia/Seoul), 현재보다 미래") LocalDateTime closesAt
) {
}
