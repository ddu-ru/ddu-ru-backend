package com.dduru.gildongmu.block.controller;

import com.dduru.gildongmu.block.dto.request.BlockedUserListRequest;
import com.dduru.gildongmu.block.dto.response.BlockedUserListResponse;
import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "User Block", description = "사용자 차단 API")
@SecurityRequirement(name = "JWT")
public interface UserBlockApiDocs {

    @Operation(summary = "사용자 차단", description = "상대 사용자를 차단합니다. 같은 사용자를 다시 차단해도 성공으로 처리합니다.")
    @ApiResponse(responseCode = "204", description = "차단 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.USER_NOT_FOUND, ErrorCode.SELF_BLOCK_NOT_ALLOWED})
    ResponseEntity<ApiResult<Void>> block(@Parameter(hidden = true) Long userId, Long targetUserId);

    @Operation(summary = "사용자 차단 해제", description = "내가 설정한 차단 관계만 해제합니다. 이미 해제된 관계도 성공으로 처리합니다.")
    @ApiResponse(responseCode = "204", description = "차단 해제 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<Void>> unblock(@Parameter(hidden = true) Long userId, Long targetUserId);

    @Operation(summary = "내 차단 목록 조회", description = "내가 차단한 사용자를 최근 차단순으로 커서 페이지 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE})
    ResponseEntity<ApiResult<BlockedUserListResponse>> getBlockedUsers(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject BlockedUserListRequest request
    );
}
