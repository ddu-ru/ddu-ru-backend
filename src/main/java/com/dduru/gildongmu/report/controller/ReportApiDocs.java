package com.dduru.gildongmu.report.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.report.dto.request.ReportCreateRequest;
import com.dduru.gildongmu.report.dto.response.ReportCreateResponse;
import com.dduru.gildongmu.report.dto.response.ReportStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Reports", description = "게시글, 사용자, 메시지 신고 API")
@SecurityRequirement(name = "JWT")
public interface ReportApiDocs {

    @Operation(summary = "본인 신고 처리 상태 조회", description = "신고자 본인만 조회합니다. 관리자 메모와 증빙은 공개하지 않습니다.")
    @ApiErrorResponses({ErrorCode.REPORT_NOT_FOUND, ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<ReportStatusResponse>> retrieveStatus(
            @Parameter(description = "신고 ID") Long reportId,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "사용자 신고", description = "본인을 제외한 사용자를 신고합니다. 처리 중인 동일 대상 신고는 중복 접수할 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "신고 접수 성공")
    @ApiErrorResponses({ErrorCode.SELF_REPORT_NOT_ALLOWED, ErrorCode.DUPLICATE_REPORT,
            ErrorCode.INVALID_REPORT_REASON, ErrorCode.INVALID_REPORT_EVIDENCE, ErrorCode.REPORT_EVIDENCE_UNAVAILABLE,
            ErrorCode.USER_NOT_FOUND, ErrorCode.INVALID_INPUT_VALUE, ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<ReportCreateResponse>> reportUser(
            @Parameter(description = "신고 대상 사용자 ID") Long targetUserId,
            @Parameter(hidden = true) Long userId,
            @Valid ReportCreateRequest request
    );

    @Operation(summary = "채팅 메시지 신고", description = "해당 방에서 열람 가능한 타인의 메시지를 신고합니다. "
            + "SYSTEM 메시지는 신고할 수 없고, 동일 메시지 신고는 1회만 가능합니다.")
    @ApiResponse(responseCode = "201", description = "신고 접수 성공")
    @ApiErrorResponses({ErrorCode.SELF_REPORT_NOT_ALLOWED, ErrorCode.DUPLICATE_REPORT,
            ErrorCode.INVALID_REPORT_REASON, ErrorCode.INVALID_REPORT_EVIDENCE, ErrorCode.REPORT_EVIDENCE_UNAVAILABLE,
            ErrorCode.INVALID_REPORT_TARGET, ErrorCode.CHAT_ACCESS_DENIED, ErrorCode.CHAT_MESSAGE_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE, ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<ReportCreateResponse>> reportMessage(
            @Parameter(description = "채팅방 ID") Long roomId,
            @Parameter(description = "신고 대상 메시지 ID") Long messageId,
            @Parameter(hidden = true) Long userId,
            @Valid ReportCreateRequest request
    );

    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다.")
    @ApiResponse(responseCode = "201", description = "신고 접수 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.SELF_POST_REPORT_NOT_ALLOWED,
            ErrorCode.DUPLICATE_POST_REPORT,
            ErrorCode.INVALID_REPORT_EVIDENCE,
            ErrorCode.REPORT_EVIDENCE_UNAVAILABLE,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<ReportCreateResponse>> createReport(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId,
            @Valid ReportCreateRequest request
    );
}
