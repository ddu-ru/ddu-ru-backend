package com.dduru.gildongmu.superhost.dto.response;

import com.dduru.gildongmu.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SuperHostManagePostResponse(
        @Schema(description = "게시글 ID", example = "101")
        Long postId,
        @Schema(description = "게시글 제목", example = "제주도 3박 4일 여행")
        String title,
        @Schema(description = "여행지 도시명", example = "제주")
        String destination,
        @Schema(description = "여행 시작일", example = "2026-03-15")
        LocalDate startDate,
        @Schema(description = "여행 종료일", example = "2026-03-18")
        LocalDate endDate,
        @Schema(description = "현재 모집 인원", example = "3")
        int recruitCount,
        @Schema(description = "모집 정원", example = "4")
        int recruitCapacity,
        @Schema(description = "대표 이미지 URL", nullable = true)
        String photoUrl,
        @Schema(description = "조회수", example = "344")
        int viewCount,
        @Schema(description = "슈퍼호스트 최종 종료 시각. 미적용 게시글은 null", nullable = true)
        LocalDateTime superHostEndsAt
) {
    public static SuperHostManagePostResponse from(Post post, LocalDateTime superHostEndsAt) {
        return new SuperHostManagePostResponse(
                post.getId(),
                post.getTitle(),
                post.getDestination().getCity(),
                post.getStartDate(),
                post.getEndDate(),
                post.getRecruitCount(),
                post.getRecruitCapacity(),
                post.getPhotoUrl(),
                post.getViewCount(),
                superHostEndsAt
        );
    }
}
