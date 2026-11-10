package com.dduru.gildongmu.admin.report.controller;

import com.dduru.gildongmu.admin.report.dto.request.AdminReportListRequest;
import com.dduru.gildongmu.admin.report.dto.request.AdminReportUpdateRequest;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportListResponse;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportResponse;
import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Reports", description = "관리자 신고 API")
@SecurityRequirement(name = "JWT")
public interface AdminReportApiDocs {

    @Operation(summary = "관리자 신고 증빙 조회", description = "ADMIN 전용. 5분간 유효한 임시 다운로드 URL을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "증빙 조회 URL 발급 성공")
    @ApiErrorResponses({
            ErrorCode.REPORT_NOT_FOUND,
            ErrorCode.INVALID_REPORT_EVIDENCE,
            ErrorCode.REPORT_EVIDENCE_UNAVAILABLE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED
    })
    ResponseEntity<ApiResult<ReportEvidenceResponse>> retrieveEvidence(
            @Parameter(description = "신고 ID") Long reportId
    );

    @Operation(summary = "신고 목록 조회", description = "게시글·사용자·메시지 신고를 상태·페이지로 조회합니다. "
            + "targetType으로 구분하며 게시글 이외의 신고는 postId와 postTitle이 null입니다. 증빙은 별도 관리자 API로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED
    })
    ResponseEntity<ApiResult<AdminReportListResponse>> listReports(
            @ParameterObject AdminReportListRequest request,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "신고 처리", description = "신고 상태·처리 메모를 갱신하고 처리자를 기록합니다. "
            + "RESOLVED 또는 REJECTED로 상태가 변경되면 신고자에게 결과 알림을 보냅니다. 같은 상태 재요청은 알림을 다시 보내지 않습니다.")
    @ApiResponse(responseCode = "200", description = "신고 처리 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.REPORT_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED
    })
    ResponseEntity<ApiResult<AdminReportResponse>> updateReport(
            @Parameter(description = "신고 ID") Long reportId,
            @Parameter(hidden = true) Long reviewerId,
            @Valid @RequestBody AdminReportUpdateRequest request
    );
}
