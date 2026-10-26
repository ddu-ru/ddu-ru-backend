package com.dduru.gildongmu.journey.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record JourneyPollListRequest(
        @Positive @Schema(description = "마지막 투표 ID. 첫 페이지는 생략") Long cursor,
        @Schema(description = "기본 20, 1~50 범위 밖이면 기본값 적용") Integer size
) {
    public JourneyPollListRequest {
        if (size == null || size < 1 || size > 50) size = 20;
    }
}
