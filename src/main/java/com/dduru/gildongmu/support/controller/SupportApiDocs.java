package com.dduru.gildongmu.support.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.support.dto.request.*;
import com.dduru.gildongmu.support.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Support", description = "고객센터 FAQ 및 본인 문의")
@SecurityRequirement(name = "JWT")
public interface SupportApiDocs {

    @Operation(
            summary = "FAQ 목록 조회",
            description = "로그인 필수. 노출 중인 FAQ를 정렬 순서·ID 오름차순으로 조회합니다. 카테고리와 질문·답변 부분 검색을 함께 적용할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE
    })
    ResponseEntity<ApiResult<List<FaqResponse>>> listFaqs(
            @ParameterObject FaqListRequest request
    );

    @Operation(
            summary = "내 문의 목록 조회",
            description = "기간 제한 없이 본인의 전체 문의를 ID 내림차순으로 조회합니다. 기본 20개, 최대 50개. nextCursor가 null이면 마지막 페이지입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE
    })
    ResponseEntity<ApiResult<InquiryListResponse>> listInquiries(
            @Parameter(hidden = true) Long userId,
            @ParameterObject InquiryListRequest request
    );

    @Operation(
            summary = "내 문의 상세 조회",
            description = "문의 본문, 순서가 있는 사진, 답변과 수정 가능 여부를 반환합니다. 답변 대기 중에는 answer가 null입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INQUIRY_NOT_FOUND,
            ErrorCode.INQUIRY_ACCESS_DENIED
    })
    ResponseEntity<ApiResult<InquiryDetailResponse>> detail(
            @Parameter(description = "문의 ID") Long id,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "문의 등록",
            description = "유형과 1~500자 본문, 선택 사진 최대 4장을 등록합니다. 문의 전용 업로드 URL의 fileUrl을 사용합니다. 제목은 따로 받지 않습니다."
    )
    @ApiResponse(responseCode = "201", description = "문의 등록 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_INQUIRY,
            ErrorCode.IMAGE_URL_INVALID_FORMAT,
            ErrorCode.IMAGE_URL_NOT_ABSOLUTE,
            ErrorCode.IMAGE_URL_INVALID_SCHEME,
            ErrorCode.IMAGE_URL_NOT_ALLOWED
    })
    ResponseEntity<ApiResult<InquiryDetailResponse>> create(
            @Parameter(hidden = true) Long userId,
            InquiryWriteRequest request
    );

    @Operation(
            summary = "문의 수정",
            description = "본인의 답변 대기 문의만 수정합니다. 유형·본문·사진을 전체 교체하며 사진 생략 또는 빈 배열은 첨부 제거입니다. 답변 완료 후에는 409를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INQUIRY_NOT_FOUND,
            ErrorCode.INQUIRY_ACCESS_DENIED,
            ErrorCode.INQUIRY_ALREADY_ANSWERED,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_INQUIRY,
            ErrorCode.IMAGE_URL_INVALID_FORMAT,
            ErrorCode.IMAGE_URL_NOT_ABSOLUTE,
            ErrorCode.IMAGE_URL_INVALID_SCHEME,
            ErrorCode.IMAGE_URL_NOT_ALLOWED
    })
    ResponseEntity<ApiResult<InquiryDetailResponse>> update(
            @Parameter(description = "문의 ID") Long id,
            @Parameter(hidden = true) Long userId,
            InquiryWriteRequest request
    );
}
