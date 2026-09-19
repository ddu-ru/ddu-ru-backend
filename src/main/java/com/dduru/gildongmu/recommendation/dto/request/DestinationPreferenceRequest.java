package com.dduru.gildongmu.recommendation.dto.request;

import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "국가 선택은 type=COUNTRY와 countryCode, 도시 선택은 type=CITY와 destinationId를 전달합니다.")
public record DestinationPreferenceRequest(
        @Schema(description = "여행지 선택 유형: COUNTRY(국가), CITY(도시)", example = "CITY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull RecommendationDestinationPreferenceType type,
        @Schema(description = "COUNTRY 선택 시 필수인 국가 코드입니다. 선호 검색 응답의 countryCode를 사용하며 CITY 선택 시 생략합니다.", example = "JP")
        String countryCode,
        @Schema(description = "CITY 선택 시 필수인 도시 ID입니다. 선호 검색 응답의 destinationId를 사용하며 COUNTRY 선택 시 생략합니다.", example = "1")
        Long destinationId
) {
}
