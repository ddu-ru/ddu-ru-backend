package com.dduru.gildongmu.recommendation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "생략하거나 null인 목록은 유지하고, 전달한 배열은 해당 목록 전체를 교체합니다. 빈 배열은 전체 삭제입니다. 배열 내부의 null 항목은 허용하지 않습니다.")
public record TravelPreferenceUpdateRequest(
        @Size(max = 3)
        @Schema(description = "입력 기준 최대 3개이며 배열 순서가 선호 순위입니다. 생략 또는 null은 기존 여행지 유지, 빈 배열은 전체 삭제입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<@NotNull @Valid DestinationPreferenceRequest> destinationPreferences,

        @Schema(description = "생략 또는 null은 기존 여행 가능 날짜 유지, 빈 배열은 전체 삭제입니다. 값이 있는 배열은 전체 교체합니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<@NotNull @Valid AvailableDateRequest> availableDates
) {
}
