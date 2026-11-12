package com.dduru.gildongmu.support.dto.response;

import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.domain.enums.*;

import java.time.LocalDateTime;

public record InquirySummaryResponse(
        Long id,
        InquiryCategory category,
        String content,
        InquiryStatus status,
        LocalDateTime createdAt
) {
    public static InquirySummaryResponse from(Inquiry inquiry) {
        return new InquirySummaryResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }
}
