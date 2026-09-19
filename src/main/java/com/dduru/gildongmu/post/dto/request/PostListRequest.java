package com.dduru.gildongmu.post.dto.request;

import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.RecruitmentStatusFilter;
import com.dduru.gildongmu.post.domain.enums.PostSortType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Locale;

public record PostListRequest(
        @Schema(description = "다음 페이지 조회용 커서 게시글 ID. 첫 페이지에서는 생략합니다.", example = "120", nullable = true)
        Long cursor,
        @Schema(description = "정렬 기준에 함께 쓰이는 보조 커서 값. 좋아요순 등에서 서버가 내려준 nextCursorValue를 그대로 전달합니다.", example = "15", nullable = true)
        Integer cursorValue,
        @Schema(description = "페이지 크기. 생략하거나 1 미만 또는 50 초과이면 10으로 보정됩니다.", example = "10")
        Integer size,
        @Schema(description = "제목/내용 검색어. 공백만 보내면 필터가 적용되지 않습니다.", example = "제주", nullable = true)
        String keyword,
        @Schema(description = "여행 시작일 필터", example = "2026-07-10", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Schema(description = "여행 종료일 필터", example = "2026-07-12", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @Schema(description = "선호 성별 필터. U는 성별 무관입니다.", example = "U", allowableValues = {"M", "F", "U"}, nullable = true)
        Gender preferredGender,
        @Schema(description = "선호 최소 나이 필터", example = "20", nullable = true)
        Integer minAge,
        @Schema(description = "선호 최대 나이 필터", example = "29", nullable = true)
        Integer maxAge,
        @Schema(description = "여행지 ID 필터", example = "1", nullable = true)
        Long destinationId,
        @Schema(description = "국가 코드 필터", example = "JP", nullable = true)
        String countryCode,
        @Schema(description = "모집 정원 최소 인원 필터", example = "2", nullable = true)
        Integer minRecruitCapacity,
        @Schema(description = "모집 정원 최대 인원 필터", example = "4", nullable = true)
        Integer maxRecruitCapacity,
        @Schema(description = "모집 상태 필터. 미전달 시 모든 모집 상태의 게시글을 반환합니다.", example = "OPEN", nullable = true)
        RecruitmentStatusFilter recruitmentStatus,
        @Schema(description = "동행 방식 필터", example = "FULL", allowableValues = {"FULL", "PARTIAL", "MEAL", "UNSPECIFIED"}, nullable = true)
        CompanionType companionType,
        @Schema(description = "정렬 기준. 미전달 시 LATEST로 처리됩니다.", example = "LATEST", nullable = true)
        PostSortType sort
) {
    public PostListRequest {
        if (size == null || size <= 0 || size > 50) size = 10;
        if (sort == null) sort = PostSortType.LATEST;
        if (keyword != null) keyword = keyword.isBlank() ? null : keyword.strip();
        if (countryCode != null) {
            countryCode = countryCode.isBlank() ? null : countryCode.strip().toUpperCase(Locale.ROOT);
        }
    }
}
