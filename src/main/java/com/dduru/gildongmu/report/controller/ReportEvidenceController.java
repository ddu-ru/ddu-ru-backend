package com.dduru.gildongmu.report.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceUploadResponse;
import com.dduru.gildongmu.report.service.ReportEvidenceService;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/evidence")
public class ReportEvidenceController implements ReportEvidenceApiDocs {

    private final ReportEvidenceService reportEvidenceService;

    @Override
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResult<ReportEvidenceUploadResponse>> prepareEvidenceUpload(
            @CurrentUser Long userId,
            @Valid @RequestBody ImageUploadFileRequest request
    ) {
        ReportEvidenceUploadResponse response = reportEvidenceService.prepareUpload(userId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
