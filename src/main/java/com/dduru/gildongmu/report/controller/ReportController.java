package com.dduru.gildongmu.report.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.report.dto.request.ReportCreateRequest;
import com.dduru.gildongmu.report.dto.response.ReportCreateResponse;
import com.dduru.gildongmu.report.dto.response.ReportStatusResponse;
import com.dduru.gildongmu.report.service.ReportQueryService;
import com.dduru.gildongmu.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReportController implements ReportApiDocs {

    private final ReportService reportService;
    private final ReportQueryService reportQueryService;

    @Override
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResult<ReportStatusResponse>> retrieveStatus(
            @PathVariable Long reportId,
            @CurrentUser Long userId
    ) {
        ReportStatusResponse response = reportQueryService.retrieveStatus(reportId, userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PostMapping("/users/{targetUserId}/reports")
    public ResponseEntity<ApiResult<ReportCreateResponse>> reportUser(
            @PathVariable Long targetUserId,
            @CurrentUser Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        ReportCreateResponse response = reportService.reportUser(targetUserId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }

    @Override
    @PostMapping("/chat/rooms/{roomId}/messages/{messageId}/reports")
    public ResponseEntity<ApiResult<ReportCreateResponse>> reportMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @CurrentUser Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        ReportCreateResponse response = reportService.reportMessage(roomId, messageId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }

    @Override
    @PostMapping("/posts/{postId}/reports")
    public ResponseEntity<ApiResult<ReportCreateResponse>> createReport(
            @PathVariable Long postId,
            @CurrentUser Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        ReportCreateResponse response = reportService.create(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }
}
