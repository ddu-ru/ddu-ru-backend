package com.dduru.gildongmu.survey.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.survey.dto.request.SurveyRequest;
import com.dduru.gildongmu.survey.dto.response.SurveyQuestionListResponse;
import com.dduru.gildongmu.survey.dto.response.SurveyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Survey", description = "설문조사 API")
public interface SurveyApiDocs {

    @Operation(
            summary = "설문조사 제출",
            description = "주요 4축(각 3문항)·활동 취향·기록 스타일을 제출하고 성향 점수 및 아바타를 매칭합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "201", description = "설문 제출 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.PROFILE_NOT_FOUND,
            ErrorCode.USER_ONBOARDING_NOT_FOUND,
            ErrorCode.AVATAR_PROFILE_NOT_FOUND
    })
    ResponseEntity<ApiResult<SurveyResponse>> createSurvey(@Parameter(hidden = true) Long userId, @Valid SurveyRequest request);

    @Operation(
            summary = "내 설문 결과 조회",
            description = "현재 사용자의 설문 결과, 매칭된 아바타, 최근 테스트일, 다시 테스트 가능 여부를 조회합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.SURVEY_RESULT_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<SurveyResponse>> getMySurveyResult(@Parameter(hidden = true) Long userId);

    @Operation(summary = "설문 문항 리스트 조회", description = "설문 문항과 선택지를 조회합니다. 인증 없이 접근 가능합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE
    })
    ResponseEntity<ApiResult<SurveyQuestionListResponse>> getSurveyQuestions();

    @Operation(
            summary = "설문 결과 수정",
            description = "마이페이지에서 기존 설문 응답을 다시 제출합니다. 가입 시 최초 테스트와 별개로 첫 재테스트는 즉시 1회 가능하며, 이후에는 마지막 수정일로부터 30일이 지난 날부터 수정할 수 있습니다. 온보딩 완료 처리 및 보상 지급은 실행되지 않습니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.SURVEY_RESULT_NOT_FOUND,
            ErrorCode.SURVEY_RETAKE_LOCKED
    })
    ResponseEntity<ApiResult<SurveyResponse>> updateSurvey(@Parameter(hidden = true) Long userId, @Valid SurveyRequest request);

    @Operation(
            summary = "설문조사 스킵",
            description = "설문조사를 건너뛰고 온보딩 상태를 SKIPPED로 업데이트합니다.",
            security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponse(responseCode = "204", description = "스킵 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_ONBOARDING_NOT_FOUND,
            ErrorCode.SURVEY_ALREADY_COMPLETED
    })
    ResponseEntity<ApiResult<Void>> skipSurvey(@Parameter(hidden = true) Long userId);
}
