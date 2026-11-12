package com.dduru.gildongmu.support.dto.request;

import com.dduru.gildongmu.support.domain.enums.FaqCategory;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record FaqListRequest(
        @Schema(description = "미지정 시 전체 카테고리")
        FaqCategory category,

        @Schema(description = "질문·답변 부분 검색, 공백만 입력하면 전체 조회")
        @Size(max = 100)
        String keyword
) {

    public FaqListRequest {
        if (keyword != null) keyword = keyword.isBlank() ? null : keyword.strip();
    }
}
