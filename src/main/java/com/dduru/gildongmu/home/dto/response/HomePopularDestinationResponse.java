package com.dduru.gildongmu.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record HomePopularDestinationResponse(
        @Schema(description = "집계 기준 시각(KST). 캐시 조회 시 유지", example = "2026-09-23T14:00:00")
        LocalDateTime updatedAt,
        @Schema(description = "인기 여행지와 기본 추천 목록, 최대 5개. 등록된 여행지가 5개 미만이면 존재하는 개수만 반환")
        List<Item> items
) {

    public enum Source { POPULAR, DEFAULT }

    public record Item(
            @Schema(description = "카드 노출 순서(1부터 시작). source가 POPULAR일 때만 순위 배지로 표시", example = "1")
            int rank,
            @Schema(description = "여행지 ID. 해당 지역 동행 목록 조회에 사용", example = "122")
            Long destinationId,
            @Schema(description = "여행지 이름", example = "제주")
            String destinationName,
            @Schema(description = "여행지 대표 이미지 URL. 없으면 null", nullable = true)
            String imageUrl,
            @Schema(description = "기간과 관계없이 삭제되지 않은 현재 OPEN 여행방 수. 개인별 참여 가능 여부는 반영하지 않음", example = "8")
            long availableTripCount,
            @Schema(description = "전체 기간 게시글의 인기 태그 최대 3개. DEFAULT는 빈 배열")
            List<String> tags,
            @Schema(description = "전체 기간에 생성된 삭제되지 않은 여행방 수(마감 포함)", example = "20")
            long totalTripCount,
            @Schema(description = "POPULAR: rank 순위 배지 표시, DEFAULT: 순위 없이 추천 여행지로 표시")
            Source source
    ) {
    }
}
