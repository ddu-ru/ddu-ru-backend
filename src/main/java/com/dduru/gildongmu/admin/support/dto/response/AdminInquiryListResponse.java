package com.dduru.gildongmu.admin.support.dto.response;

import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.dto.response.InquirySummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminInquiryListResponse(
        List<InquirySummaryResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean last
) {

    public static AdminInquiryListResponse from(Page<Inquiry> page) {
        return new AdminInquiryListResponse(
                page.map(InquirySummaryResponse::from).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isLast()
        );
    }
}
