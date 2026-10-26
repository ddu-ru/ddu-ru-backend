package com.dduru.gildongmu.journey.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.journey.dto.request.JourneyPollCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollListRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollOptionCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollVoteRequest;
import com.dduru.gildongmu.journey.dto.response.JourneyPollListResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Journey Polls", description = "나의 여정 투표 API. 활성 여정 멤버만 접근 가능합니다.")
@SecurityRequirement(name = "JWT")
public interface JourneyPollApiDocs {
    @Operation(summary = "투표 목록 조회", description = "투표 ID 내림차순 커서 조회. 종료된 투표도 포함합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE, ErrorCode.JOURNEY_ACCESS_DENIED})
    ResponseEntity<ApiResult<JourneyPollListResponse>> retrievePolls(
            Long journeyId, @Parameter(hidden = true) Long userId, @ParameterObject JourneyPollListRequest request);

    @Operation(summary = "투표 상세 조회", description = "투표 전에도 집계를 공개합니다. 익명 투표는 투표자 신원을 반환하지 않고 본인 선택만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.JOURNEY_ACCESS_DENIED, ErrorCode.JOURNEY_POLL_NOT_FOUND})
    ResponseEntity<ApiResult<JourneyPollResponse>> retrievePoll(Long journeyId, Long pollId, @Parameter(hidden = true) Long userId);

    @Operation(summary = "투표 생성", description = "선택지 2~10개, 제목/선택지 최대 100자. 활성 그룹 채팅방에 POLL 메시지를 함께 생성합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE, ErrorCode.JOURNEY_ACCESS_DENIED,
            ErrorCode.JOURNEY_NOT_FOUND, ErrorCode.JOURNEY_POLL_INVALID_TEXT, ErrorCode.JOURNEY_POLL_INVALID_OPTIONS,
            ErrorCode.JOURNEY_POLL_INVALID_CLOSING_TIME, ErrorCode.CHAT_ROOM_NOT_FOUND,
            ErrorCode.CHAT_ROOM_CLOSED, ErrorCode.CHAT_ACCESS_DENIED})
    ResponseEntity<ApiResult<JourneyPollResponse>> createPoll(
            Long journeyId, @Parameter(hidden = true) Long userId, JourneyPollCreateRequest request);

    @Operation(summary = "투표 및 재투표", description = "내 선택 전체를 교체합니다. 같은 선택 재요청은 멱등 처리하며 종료 시각 이상이면 거절합니다.")
    @ApiResponse(responseCode = "200", description = "투표 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE, ErrorCode.JOURNEY_ACCESS_DENIED,
            ErrorCode.JOURNEY_POLL_NOT_FOUND, ErrorCode.JOURNEY_POLL_CLOSED, ErrorCode.JOURNEY_POLL_INVALID_SELECTION})
    ResponseEntity<ApiResult<JourneyPollResponse>> vote(
            Long journeyId, Long pollId, @Parameter(hidden = true) Long userId, JourneyPollVoteRequest request);

    @Operation(summary = "선택지 추가", description = "선택지 추가 허용 투표에 활성 멤버가 추가합니다. 종료 후 추가 및 중복 내용, 총 10개 초과는 불가합니다.")
    @ApiResponse(responseCode = "201", description = "추가 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE, ErrorCode.JOURNEY_ACCESS_DENIED,
            ErrorCode.JOURNEY_POLL_NOT_FOUND, ErrorCode.JOURNEY_POLL_CLOSED, ErrorCode.JOURNEY_POLL_INVALID_TEXT,
            ErrorCode.JOURNEY_POLL_INVALID_OPTIONS, ErrorCode.JOURNEY_POLL_OPTION_ADDITION_DENIED})
    ResponseEntity<ApiResult<JourneyPollResponse>> addOption(
            Long journeyId, Long pollId, @Parameter(hidden = true) Long userId, JourneyPollOptionCreateRequest request);
}
