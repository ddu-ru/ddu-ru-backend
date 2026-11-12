package com.dduru.gildongmu.support.dto.request;

import com.dduru.gildongmu.support.domain.enums.InquiryCategory;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record InquiryWriteRequest(
        @NotNull
        InquiryCategory category,

        @NotBlank
        @Size(max = 500)
        String content,

        @Schema(description = "첨부 사진 URL, 최대 4장. 생략 또는 빈 배열이면 첨부 없음. PUT 시 전체 교체")
        @Size(max = 4)
        List<@NotBlank @Size(max = 2048) String> imageUrls
) {

    public InquiryWriteRequest {
        if (content != null) content = content.strip();
        imageUrls = imageUrls == null ? List.of() : imageUrls.stream()
                .map(url -> url == null ? null : url.strip())
                .toList();
    }
}
