package com.dduru.gildongmu.post.dto.request;

import com.dduru.gildongmu.post.domain.enums.MyPagePostFilter;
import com.dduru.gildongmu.post.domain.enums.MyPagePostSortType;
import io.swagger.v3.oas.annotations.media.Schema;

public record MyPagePostListRequest(
        @Schema(description = "다음 페이지 조회용 커서 게시글 ID. 첫 페이지에서는 생략합니다.", example = "120", nullable = true)
        Long cursor,
        @Schema(description = "페이지 크기. 생략하거나 1 미만 또는 50 초과이면 10으로 보정됩니다.", example = "10")
        Integer size,
        @Schema(description = "상태 필터. 생략 시 전체 조회합니다.", example = "ALL",
                allowableValues = {"ALL", "RECRUITING", "RECRUITMENT_CLOSED", "TRAVEL_ENDED"})
        MyPagePostFilter status,
        @Schema(description = "정렬 기준. 생략 시 최신순입니다.", example = "LATEST",
                allowableValues = {"LATEST", "OLDEST"})
        MyPagePostSortType sort
) {
    public MyPagePostListRequest {
        if (size == null || size <= 0 || size > 50) size = 10;
        if (status == null) status = MyPagePostFilter.ALL;
        if (sort == null) sort = MyPagePostSortType.LATEST;
    }
}
