package com.dduru.gildongmu.admin.report.controller;

import com.dduru.gildongmu.admin.report.dto.request.AdminReportListRequest;
import com.dduru.gildongmu.admin.report.dto.request.AdminReportUpdateRequest;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportListResponse;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportResponse;
import com.dduru.gildongmu.admin.report.service.AdminReportService;
import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceResponse;
import com.dduru.gildongmu.report.service.ReportEvidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController implements AdminReportApiDocs {

    private final AdminReportService adminReportService;
    private final ReportEvidenceService reportEvidenceService;

    @Override
    @GetMapping("/{reportId}/evidence")
    public ResponseEntity<ApiResult<ReportEvidenceResponse>> retrieveEvidence(@PathVariable Long reportId) {
        ReportEvidenceResponse response = reportEvidenceService.retrieveEvidence(reportId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<AdminReportListResponse>> listReports(
            AdminReportListRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        AdminReportListResponse response = adminReportService.findAll(request.status(), pageable);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PatchMapping("/{reportId}")
    public ResponseEntity<ApiResult<AdminReportResponse>> updateReport(
            @PathVariable Long reportId,
            @CurrentUser Long reviewerId,
            @Valid @RequestBody AdminReportUpdateRequest request
    ) {
        AdminReportResponse response = adminReportService.update(reportId, reviewerId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
