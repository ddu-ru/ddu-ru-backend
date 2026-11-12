package com.dduru.gildongmu.support.dto.response;

import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.domain.InquiryImage;
import com.dduru.gildongmu.support.domain.enums.*;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryDetailResponse(
        Long id,
        InquiryCategory category,
        String content,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        List<String> imageUrls,
        boolean editable,
        AnswerResponse answer
) {
    public record AnswerResponse(
            String content,
            LocalDateTime answeredAt,
            LocalDateTime modifiedAt
    ) {}

    public static InquiryDetailResponse from(Inquiry inquiry) {
        AnswerResponse answer = inquiry.getAnsweredAt() == null ? null : new AnswerResponse(
                inquiry.getAnswerContent(), inquiry.getAnsweredAt(), inquiry.getAnswerModifiedAt());
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getModifiedAt(),
                inquiry.getImages().stream().map(InquiryImage::getImageUrl).toList(),
                inquiry.isEditable(),
                answer
        );
    }
}
