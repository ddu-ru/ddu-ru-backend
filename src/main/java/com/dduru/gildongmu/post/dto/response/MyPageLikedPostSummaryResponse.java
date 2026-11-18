package com.dduru.gildongmu.post.dto.response;

import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record MyPageLikedPostSummaryResponse(
        @Schema(description = "게시글 ID", example = "101")
        Long id,
        @Schema(description = "게시글 제목", example = "제주도 2박 3일 동행 구해요")
        String title,
        @Schema(description = "여행지 도시명", example = "제주")
        String destination,
        @Schema(description = "여행 시작일", example = "2026-07-10")
        LocalDate startDate,
        @Schema(description = "여행 종료일", example = "2026-07-12")
        LocalDate endDate,
        @Schema(description = "현재 참여 확정 인원 수", example = "3")
        int recruitCount,
        @Schema(description = "모집 정원. 호스트를 포함한 총 인원입니다.", example = "4")
        int recruitCapacity,
        @Schema(description = "선호 성별. U는 성별 무관입니다.", example = "U", allowableValues = {"M", "F", "U"})
        Gender preferredGender,
        @Schema(description = "동행 방식", example = "FULL", allowableValues = {"FULL", "PARTIAL", "MEAL", "UNSPECIFIED"})
        CompanionType companionType,
        @Schema(description = "게시글 대표 이미지 URL. 없으면 null입니다.", nullable = true)
        String photoUrl,
        @Schema(description = "작성자 정보")
        PostSummaryAuthorInfo author
) {
    public static MyPageLikedPostSummaryResponse from(Post post, boolean isSuperHost) {
        return new MyPageLikedPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getDestination().getCity(),
                post.getStartDate(),
                post.getEndDate(),
                post.getRecruitCount(),
                post.getRecruitCapacity(),
                post.getPreferredGender(),
                post.getCompanionType(),
                post.getPhotoUrl(),
                PostSummaryAuthorInfo.from(post.getUser(), isSuperHost)
        );
    }
}
