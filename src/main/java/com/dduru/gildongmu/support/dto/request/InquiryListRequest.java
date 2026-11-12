package com.dduru.gildongmu.support.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record InquiryListRequest(
        @Schema(description = "이전 응답 nextCursor. 첫 조회 생략")
        @Positive
        Long cursor,

        @Schema(description = "기본 20, 최대 50")
        @Min(1)
        @Max(50)
        Integer size
) {

    public int sizeOrDefault() {
        return size == null ? 20 : size;
    }
}
