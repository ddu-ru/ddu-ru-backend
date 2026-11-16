package com.dduru.gildongmu.post.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.post.dto.request.PostCreateRequest;
import com.dduru.gildongmu.post.dto.request.PostListRequest;
import com.dduru.gildongmu.post.dto.response.PostCreateResponse;
import com.dduru.gildongmu.post.dto.response.PostDetailResponse;
import com.dduru.gildongmu.post.dto.response.PostListResponse;
import com.dduru.gildongmu.post.dto.request.PostStatusUpdateRequest;
import com.dduru.gildongmu.post.dto.request.PostUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Posts", description = "여행 게시글 API")
public interface PostApiDocs {

    @Operation(summary = "게시글 목록 조회", description = """
            필터 조건에 따라 게시글 목록을 조회합니다.

            recruitmentStatus 미전달 시 모집 상태와 관계없이 삭제되지 않은 모든 게시글을 반환합니다.
            - OPEN: 모집 중 (status=OPEN, 인원 미달)
            - DEADLINE_NEAR: 마감 임박 (모집 중 + 모집 마감일이 오늘부터 3일 이내)
            - CLOSED: 모집 완료 (인원 마감 또는 호스트 수동 마감)
            countryCode로 국가 단위 여행지를 필터링할 수 있습니다.
            minRecruitCapacity와 maxRecruitCapacity로 모집 정원 범위를 필터링할 수 있습니다.
            """)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<PostListResponse>> retrievePosts(@ParameterObject PostListRequest request, @Parameter(hidden = true) Long userId);

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.POST_NOT_FOUND})
    ResponseEntity<ApiResult<PostDetailResponse>> retrievePostDetail(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.", security = @SecurityRequirement(name = "JWT"))
    @ApiResponse(responseCode = "201", description = "작성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_POST_DATE,
            ErrorCode.INVALID_PREFERRED_AGE,
            ErrorCode.INVALID_POST_TAGS,
            ErrorCode.DESTINATION_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<PostCreateResponse>> createPost(
            @Parameter(hidden = true) Long userId,
            @Valid PostCreateRequest request
    );

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다.", security = @SecurityRequirement(name = "JWT"))
    @ApiResponse(responseCode = "204", description = "수정 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.INVALID_POST_DATE,
            ErrorCode.INVALID_PREFERRED_AGE,
            ErrorCode.INVALID_POST_TAGS,
            ErrorCode.DESTINATION_NOT_FOUND,
            ErrorCode.TRAVEL_ALREADY_STARTED,
            ErrorCode.TRAVEL_ALREADY_ENDED,
            ErrorCode.RECRUIT_DEADLINE_PASSED,
            ErrorCode.INVALID_RECRUIT_CAPACITY,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<Void>> updatePost(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId,
            @Valid PostUpdateRequest request
    );

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.", security = @SecurityRequirement(name = "JWT"))
    @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<Void>> deletePost(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "게시글 모집 상태 변경", description = "게시글 모집 상태를 변경합니다. (true: 모집중, false: 모집마감)", security = @SecurityRequirement(name = "JWT"))
    @ApiResponse(responseCode = "204", description = "상태 변경 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<Void>> updatePostStatus(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(hidden = true) Long userId,
            @Valid PostStatusUpdateRequest request
    );

}
