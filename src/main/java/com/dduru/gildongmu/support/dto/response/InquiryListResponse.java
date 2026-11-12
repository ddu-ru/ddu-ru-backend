package com.dduru.gildongmu.support.dto.response;

import com.dduru.gildongmu.support.domain.Inquiry;

import java.util.List;

public record InquiryListResponse(
        List<InquirySummaryResponse> inquiries,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    public static InquiryListResponse of(List<Inquiry> fetched, int requestedSize) {
        boolean hasNext = fetched.size() > requestedSize;
        List<Inquiry> items = hasNext ? fetched.subList(0, requestedSize) : fetched;
        return new InquiryListResponse(
                items.stream().map(InquirySummaryResponse::from).toList(),
                hasNext ? items.get(items.size() - 1).getId() : null,
                hasNext,
                items.size()
        );
    }
}
