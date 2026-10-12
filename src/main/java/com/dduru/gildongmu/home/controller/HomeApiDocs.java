package com.dduru.gildongmu.home.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.dto.response.HomeResponse;
import com.dduru.gildongmu.home.dto.response.HomeSuperHostResponse;
import com.dduru.gildongmu.home.dto.response.MateRecommendationResponse;
import com.dduru.gildongmu.home.dto.response.SameAgeTripResponse;
import com.dduru.gildongmu.home.dto.response.SameDestinationTripResponse;
import com.dduru.gildongmu.home.dto.response.UpcomingTripResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Home", description = "홈 API")
public interface HomeApiDocs {

    @Operation(
            summary = "홈 초기 구성 조회",
            description = """
                    홈 화면 진입에 필요한 사용자 상태와 섹션 호출 정보를 조회합니다.

                    실제 섹션 데이터는 sections[].endpoint로 내려가는 API를 클라이언트가 별도로 호출합니다.
                    모든 홈 섹션 데이터는 /api/v1/home/* 홈 전용 API로 조회합니다.
                    enabled=false인 섹션은 클라이언트가 호출하지 않고 disabledReason을 기준으로 UI를 처리합니다.
                    진행·예정 여행은 활성 참여 중이고 종료일이 지나지 않은 여정이 있을 때만 활성화됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<HomeResponse>> retrieveHome(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "홈 진행·예정 여행 섹션 조회", description = "홈에서 진행 중이거나 가장 가까운 예정 여행을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.CURRENT_OR_UPCOMING_JOURNEY_NOT_FOUND
    })
    ResponseEntity<ApiResult<UpcomingTripResponse>> retrieveUpcomingTrip(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "홈 인기 여행지 섹션 조회",
            description = """
                    누적 여행방 수 기준으로 인기 여행지 최대 5개를 조회합니다.
                    부족한 항목은 기본 여행지로 보충하며, 결과는 1시간 동안 캐시됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<HomePopularDestinationResponse>> retrievePopularDestinations();

    @Operation(
            summary = "홈 메이트 추천 섹션 조회",
            description = """
                    로그인 회원의 당일 메이트 추천을 조회합니다. 같은 날의 추천은 재사용합니다.
                    추천 준비 상태는 availabilityStatus로 확인합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "추천 이용 상태와 현재 노출 가능한 카드")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.PROFILE_NOT_FOUND,
            ErrorCode.RECOMMENDATION_TENDENCY_MISSING,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    ResponseEntity<ApiResult<MateRecommendationResponse>> retrieveMateRecommendations(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "홈 슈퍼호스트 섹션 조회",
            description = "현재 참여 가능한 슈퍼호스트 게시글을 무작위로 최대 5개 조회합니다. 없으면 빈 배열을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<List<HomeSuperHostResponse>>> retrieveSuperHosts(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "홈 같은 여행지 여행 섹션 조회", description = "1순위 선호 여행지의 모집 가능한 동행을 최대 3개 조회합니다. 선호나 후보가 없으면 빈 배열을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<SameDestinationTripResponse>>> retrieveSameDestinationTrips(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "홈 또래 여행 섹션 조회", description = "작성자와 만 나이 차이가 ±5세 이내인 모집 가능한 동행을 최대 3개 조회합니다. 없으면 빈 배열을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.BIRTHDAY_NOT_FOUND
    })
    ResponseEntity<ApiResult<List<SameAgeTripResponse>>> retrieveSameAgeTrips(
            @Parameter(hidden = true) Long userId
    );
}
