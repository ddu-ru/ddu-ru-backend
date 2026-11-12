package com.dduru.gildongmu.admin.support.controller;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.dto.response.AdminInquiryListResponse;
import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.support.domain.enums.InquiryStatus;
import com.dduru.gildongmu.support.dto.response.InquiryDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Support", description = "관리자 고객센터 문의 처리")
@SecurityRequirement(name = "JWT")
public interface AdminInquiryApiDocs {

    @Operation(
            summary = "관리자 문의 목록",
            description = "전체 사용자의 문의를 ID 내림차순으로 조회합니다. 상태 생략 시 전체. page는 0부터, size 기본 20·최대 50. sort는 적용하지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.INVALID_INPUT_VALUE
    })
    ResponseEntity<ApiResult<AdminInquiryListResponse>> list(
            @Parameter(description = "문의 상태") InquiryStatus status,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "관리자 문의 상세",
            description = "기간 제한 없이 문의와 사진·답변을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.INQUIRY_NOT_FOUND
    })
    ResponseEntity<ApiResult<InquiryDetailResponse>> detail(
            @Parameter(description = "문의 ID") Long id
    );

    @Operation(
            summary = "문의 답변 등록·수정",
            description = "1~5000자 답변을 저장하고 ANSWERED로 전환합니다. 기존 답변 수정 시 최초 답변 시각은 유지합니다. 알림은 발송하지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "답변 저장 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.INQUIRY_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_INQUIRY_ANSWER
    })
    ResponseEntity<ApiResult<InquiryDetailResponse>> answer(
            @Parameter(description = "문의 ID") Long id,
            @Parameter(hidden = true) Long userId,
            InquiryAnswerRequest request
    );
}
