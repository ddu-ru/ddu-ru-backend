package com.dduru.gildongmu.support.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.support.dto.request.*;
import com.dduru.gildongmu.support.dto.response.*;
import com.dduru.gildongmu.support.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SupportController implements SupportApiDocs {

    private final FaqService faqService;
    private final InquiryService inquiryService;

    @Override
    @GetMapping("/faqs")
    public ResponseEntity<ApiResult<List<FaqResponse>>> listFaqs(@Valid @ModelAttribute FaqListRequest request) {
        return ResponseEntity.ok(ApiResult.ok(faqService.list(request)));
    }

    @Override
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResult<InquiryListResponse>> listInquiries(
            @CurrentUser Long userId,
            @Valid @ModelAttribute InquiryListRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(inquiryService.list(userId, request)));
    }

    @Override
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<ApiResult<InquiryDetailResponse>> detail(
            @PathVariable Long id,
            @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResult.ok(inquiryService.detail(id, userId)));
    }

    @Override
    @PostMapping("/inquiries")
    public ResponseEntity<ApiResult<InquiryDetailResponse>> create(
            @CurrentUser Long userId,
            @Valid @RequestBody InquiryWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.created(inquiryService.create(userId, request)));
    }

    @Override
    @PutMapping("/inquiries/{id}")
    public ResponseEntity<ApiResult<InquiryDetailResponse>> update(
            @PathVariable Long id,
            @CurrentUser Long userId,
            @Valid @RequestBody InquiryWriteRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(inquiryService.update(id, userId, request)));
    }
}
