package com.dduru.gildongmu.admin.support.controller;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.dto.response.AdminInquiryListResponse;
import com.dduru.gildongmu.admin.support.service.AdminInquiryService;
import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.support.domain.enums.InquiryStatus;
import com.dduru.gildongmu.support.dto.response.InquiryDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/inquiries")
public class AdminInquiryController implements AdminInquiryApiDocs {

    private final AdminInquiryService service;

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<AdminInquiryListResponse>> list(
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.ok(service.findAll(status, pageable)));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<InquiryDetailResponse>> detail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(service.findById(id)));
    }

    @Override
    @PutMapping("/{id}/answer")
    public ResponseEntity<ApiResult<InquiryDetailResponse>> answer(
            @PathVariable Long id,
            @CurrentUser Long userId,
            @Valid @RequestBody InquiryAnswerRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(service.updateAnswer(id, userId, request)));
    }
}
