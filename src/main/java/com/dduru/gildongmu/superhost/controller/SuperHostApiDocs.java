package com.dduru.gildongmu.superhost.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.superhost.dto.response.MySuperHostStatusResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostApplyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "SuperHost", description = "슈퍼호스트 API")
@SecurityRequirement(name = "JWT")
public interface SuperHostApiDocs {

    @Operation(summary = "내 슈퍼호스트 관리 조회", description = "보유 티켓 수와 적용 중·미적용 게시글 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<MySuperHostStatusResponse>> retrieveMySuperHostStatus(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "슈퍼호스트 티켓 적용 또는 연장", description = "보유한 티켓 1개를 게시글에 적용하거나 적용 중인 게시글의 노출 기간을 연장합니다.")
    @ApiResponse(responseCode = "200", description = "적용 성공")
    @ApiErrorResponses({
            ErrorCode.SUPER_HOST_TICKET_NOT_FOUND,
            ErrorCode.SUPER_HOST_POST_NOT_APPLICABLE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<SuperHostApplyResponse>> applySuperHostTicket(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId
    );
}
