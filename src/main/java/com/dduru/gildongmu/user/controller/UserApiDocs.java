package com.dduru.gildongmu.user.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.post.dto.request.MyPageLikedPostListRequest;
import com.dduru.gildongmu.post.dto.request.MyPagePostListRequest;
import com.dduru.gildongmu.post.dto.response.MyPageLikedPostListResponse;
import com.dduru.gildongmu.post.dto.response.MyPagePostListResponse;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "사용자 마이페이지 API")
public interface UserApiDocs {

    @Operation(
            summary = "내가 작성한 게시글 목록 조회",
            description = "내가 작성한 게시글 목록을 커서 기반 페이지네이션으로 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<MyPagePostListResponse>> retrieveMyPosts(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject MyPagePostListRequest request
    );

    @Operation(
            summary = "찜한 여행 목록 조회",
            description = "내가 찜한 여행글 목록을 커서 기반 페이지네이션으로 조회합니다. 삭제된 게시글은 제외됩니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<MyPageLikedPostListResponse>> retrieveMyLikedPosts(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject MyPageLikedPostListRequest request
    );

    @Operation(
            summary = "여행 선호 설정 조회",
            description = "선택된 여행지(국가/도시)를 preferenceRank 오름차순으로 조회하며 여행 가능 날짜 범위를 함께 반환합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<TravelPreferenceResponse>> getTravelPreferences(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "여행 선호 설정 부분 수정",
            description = "생략하거나 null인 목록은 유지하고 전달한 배열만 해당 목록 전체를 교체합니다. " +
                    "destinationPreferences는 최대 3개이며 중복 제거 후 배열 순서대로 순위를 부여합니다. " +
                    "국가 선택은 type=COUNTRY와 countryCode, 도시 선택은 type=CITY와 destinationId를 전달합니다. " +
                    "빈 배열은 해당 목록 전체 삭제입니다. 배열 내부의 null 항목은 허용하지 않습니다. 빈 객체는 변경하지 않습니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TravelPreferenceUpdateRequest.class),
                            examples = {
                                    @ExampleObject(name = "COUNTRY", summary = "국가 선택: 일본 (기존 날짜 유지)", value = """
                                            {
                                              "destinationPreferences": [
                                                { "type": "COUNTRY", "countryCode": "JP" }
                                              ]
                                            }
                                            """),
                                    @ExampleObject(name = "CITY", summary = "도시 선택 (기존 날짜 유지)", description = "destinationId는 선호 검색 API에서 받은 실제 도시 ID로 변경하세요.", value = """
                                            {
                                              "destinationPreferences": [
                                                { "type": "CITY", "destinationId": 1 }
                                              ]
                                            }
                                            """)
                            }
                    )
            ),
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "204", description = "수정 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.DESTINATION_NOT_FOUND,
            ErrorCode.INVALID_DESTINATION_PREFERENCE,
            ErrorCode.INVALID_AVAILABLE_DATE,
            ErrorCode.DUPLICATE_AVAILABLE_DATE
    })
    ResponseEntity<ApiResult<Void>> updateTravelPreferences(
            @Parameter(hidden = true) Long userId,
            @Valid TravelPreferenceUpdateRequest request
    );

}
