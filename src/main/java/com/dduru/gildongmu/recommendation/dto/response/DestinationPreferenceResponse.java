package com.dduru.gildongmu.recommendation.dto.response;

import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "여행지 선호 항목")
public record DestinationPreferenceResponse(
        @Schema(description = "선호 타입 (COUNTRY: 국가, CITY: 도시)", example = "CITY")
        RecommendationDestinationPreferenceType type,
        @Schema(description = "국가 코드", example = "JP")
        String countryCode,
        @Schema(description = "국가명", example = "일본")
        String countryName,
        @Schema(description = "도시 ID (CITY 타입만)", example = "1")
        Long destinationId,
        @Schema(description = "도시명 (CITY 타입만)", example = "도쿄")
        String city,
        @Schema(description = "선호 순위 (1~3)", example = "1")
        int preferenceRank
) {
    public static DestinationPreferenceResponse from(
            UserRecommendationDestinationPreference preference,
            Map<String, String> countryNameByCode
    ) {
        if (preference.getPreferenceType() == RecommendationDestinationPreferenceType.COUNTRY) {
            return new DestinationPreferenceResponse(
                    RecommendationDestinationPreferenceType.COUNTRY,
                    preference.getCountryCode(),
                    countryNameByCode.get(preference.getCountryCode()),
                    null,
                    null,
                    preference.getPreferenceRank()
            );
        }
        Destination dest = preference.getDestination();
        return new DestinationPreferenceResponse(
                RecommendationDestinationPreferenceType.CITY,
                dest.getCountryCode(),
                dest.getCountryName(),
                dest.getId(),
                dest.getCity(),
                preference.getPreferenceRank()
        );
    }
}
