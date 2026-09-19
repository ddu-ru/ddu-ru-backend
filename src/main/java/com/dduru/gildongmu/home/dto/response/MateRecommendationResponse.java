package com.dduru.gildongmu.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record MateRecommendationResponse(
        @Schema(description = "AVAILABLE: 완료(빈 결과 포함), GENERATING: 생성 중, SURVEY_REQUIRED: 온보딩·설문 미완료")
        AvailabilityStatus availabilityStatus,
        @Schema(description = "당일 추가 추천은 제공하지 않으므로 항상 0", example = "0")
        int remainingFreeCount,
        @Schema(description = "저장 순위 오름차순의 현재 노출 가능한 카드. 생성 중·설문 미완료·빈 결과는 빈 배열")
        List<Item> recommendations
) {
    public static MateRecommendationResponse surveyRequired() {
        return new MateRecommendationResponse(
                AvailabilityStatus.SURVEY_REQUIRED,
                0,
                List.of()
        );
    }

    public static MateRecommendationResponse generating() {
        return new MateRecommendationResponse(
                AvailabilityStatus.GENERATING,
                0,
                List.of()
        );
    }

    public static MateRecommendationResponse available(List<Item> recommendations) {
        return new MateRecommendationResponse(
                AvailabilityStatus.AVAILABLE,
                0,
                recommendations
        );
    }

    public enum AvailabilityStatus {
        AVAILABLE,
        GENERATING,
        SURVEY_REQUIRED
    }

    public record Item(
            Long recommendationId,
            Long postId,
            int matchPercentage,
            String title,
            @Schema(description = "현재 게시글의 대표 사진 URL. 사진이 없으면 null", nullable = true,
                    example = "https://example.com/trips/jeju.jpg")
            String thumbnailUrl,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            HomeHostResponse host,
            int currentMemberCount,
            int maxMemberCount,
            String description,
            List<String> tags,
            List<Reason> matchReasons,
            List<Reason> cautionPoints
    ) {
    }

    public record Reason(
            String code,
            String message
    ) {
    }
}
