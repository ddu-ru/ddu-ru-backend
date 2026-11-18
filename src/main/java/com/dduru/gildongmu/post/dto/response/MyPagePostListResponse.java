package com.dduru.gildongmu.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MyPagePostListResponse(
        @Schema(description = "게시글 목록")
        List<MyPagePostSummaryResponse> posts,
        @Schema(description = "다음 페이지 조회용 커서 게시글 ID. hasNext=false이면 null입니다.", example = "101", nullable = true)
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "현재 응답에 포함된 게시글 수", example = "10")
        int size,
        @Schema(description = "삭제되지 않은 내 전체 게시글 수. 전체보기 탭의 '전체 N' 표시에 사용됩니다.", example = "4")
        long totalPostCount,
        @Schema(description = "미사용 슈퍼호스트 티켓 수", example = "3")
        int unusedSuperHostTicketCount
) {
    public static MyPagePostListResponse of(
            List<MyPagePostSummaryResponse> posts,
            boolean hasNext,
            long totalPostCount,
            int unusedSuperHostTicketCount
    ) {
        Long nextCursor = hasNext && !posts.isEmpty() ? posts.get(posts.size() - 1).id() : null;
        return new MyPagePostListResponse(
                posts,
                nextCursor,
                hasNext,
                posts.size(),
                totalPostCount,
                unusedSuperHostTicketCount
        );
    }
}
