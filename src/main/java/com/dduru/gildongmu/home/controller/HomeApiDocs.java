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
            description = "홈 인기 여행지 섹션 데이터를 조회합니다. 현재는 화면 연동을 위한 mock 데이터 5개를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<HomePopularDestinationResponse>> retrievePopularDestinations();

    @Operation(
            summary = "홈 메이트 추천 섹션 조회",
            description = """
                    로그인 회원의 KST 당일 추천 묶음을 최초 호출에서 생성하고 같은 날에는 재사용합니다.
                    전날 추천으로 대체하지 않으며, 저장 순위 오름차순으로 현재 노출 가능한 여행방을 반환합니다.
                    게시글·호스트 정보는 현재 값, 적합도·추천 이유는 생성 시 저장값입니다.
                    생성 중은 GENERATING, 온보딩 또는 설문 미완료는 SURVEY_REQUIRED와 빈 목록을 반환합니다.
                    완료는 AVAILABLE이며 후보가 없거나 모든 카드가 숨겨졌어도 AVAILABLE과 빈 목록입니다.
                    remainingFreeCount는 항상 0이고 숨겨진 카드를 대체 생성하지 않습니다.
                    생성·조회 오류는 해당 섹션의 오류 응답으로 반환되며 다른 홈 섹션은 별도로 조회할 수 있습니다.
                    생성 실패로 FAILED가 된 당일 묶음은 다음 호출에서 재시도합니다.
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
            description = "홈 슈퍼호스트 섹션 데이터를 조회합니다. 현재는 화면 연동을 위한 mock 데이터 5개를 반환하며 hasLiked는 false입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<ApiResult<List<HomeSuperHostResponse>>> retrieveSuperHosts(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "홈 같은 여행지 여행 섹션 조회", description = "1순위 선호 도시 또는 국가의 모집 가능한 게시글을 오래된 순으로 최대 30개 고른 뒤, 그중 무작위 3개를 반환합니다. 선호나 후보가 없으면 빈 배열이며 2·3순위로 보충하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<SameDestinationTripResponse>>> retrieveSameDestinationTrips(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "홈 또래 여행 섹션 조회", description = "오늘 기준 사용자와 작성자의 만 나이 차이가 ±5세 이내인 모집 가능한 게시글을 오래된 순으로 최대 30개 고른 뒤, 그중 무작위 3개를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.BIRTHDAY_NOT_FOUND
    })
    ResponseEntity<ApiResult<List<SameAgeTripResponse>>> retrieveSameAgeTrips(
            @Parameter(hidden = true) Long userId
    );
}
