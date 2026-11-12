package com.dduru.gildongmu.admin.support.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryAnswerRequest(
        @NotBlank
        @Size(max = 5000)
        String content
) {

    public InquiryAnswerRequest {
        if (content != null) content = content.strip();
    }
}
