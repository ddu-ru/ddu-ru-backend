package com.dduru.gildongmu.journey.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record JourneyPollOptionCreateRequest(
        @NotBlank @Schema(description = "추가할 선택지. 최대 100자") String content
) {
}
