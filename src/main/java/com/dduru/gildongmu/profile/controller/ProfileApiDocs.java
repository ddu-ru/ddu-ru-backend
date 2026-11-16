package com.dduru.gildongmu.profile.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.profile.dto.request.NicknameUpdateRequest;
import com.dduru.gildongmu.profile.dto.request.ProfileSetupRequest;
import com.dduru.gildongmu.profile.dto.request.ProfileUpdateRequest;
import com.dduru.gildongmu.profile.dto.response.MyProfileResponse;
import com.dduru.gildongmu.profile.dto.response.NicknameRandomResponse;
import com.dduru.gildongmu.profile.dto.response.NicknameValidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Profiles", description = "사용자 프로필 관리 API")
@SecurityRequirement(name = "JWT")
public interface ProfileApiDocs {

    @Operation(
            summary = "마이페이지 프로필 조회",
            description = "내 프로필, 선호 여행지, 여행 가능 날짜, 여행 스타일 요약을 조회합니다. " +
                    "설문을 완료하지 않은 경우 travelStyleSummary는 null입니다."
    )
    @ApiResponse(responseCode = "200", description = "프로필 조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.PROFILE_NOT_FOUND
    })
    ResponseEntity<ApiResult<MyProfileResponse>> getMyProfile(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다.")
    @ApiResponse(responseCode = "204", description = "닉네임 수정 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.NICKNAME_INVALID_LENGTH,
            ErrorCode.NICKNAME_INVALID_CHARACTERS,
            ErrorCode.NICKNAME_CONTAINS_BAD_WORD,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.PROFILE_NOT_FOUND,
            ErrorCode.NICKNAME_ALREADY_TAKEN
    })
    ResponseEntity<ApiResult<Void>> updateNickname(
            @Parameter(hidden = true) Long id,
            @Valid NicknameUpdateRequest request
    );

    @Operation(summary = "닉네임 유효성 확인", description = "닉네임의 사용 가능 여부를 확인합니다. 닉네임 형식 검증 및 중복 여부를 체크합니다.")
    @ApiResponse(responseCode = "200", description = "닉네임 유효성 확인 성공(사용가능)")
    @ApiErrorResponses({
            ErrorCode.NICKNAME_INVALID_LENGTH,
            ErrorCode.NICKNAME_INVALID_CHARACTERS,
            ErrorCode.NICKNAME_CONTAINS_BAD_WORD,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.NICKNAME_ALREADY_TAKEN
    })
    ResponseEntity<ApiResult<NicknameValidateResponse>> checkNickname(
            @Parameter(
                    description = "유효성 체크할 닉네임",
                    example = "길동무"
            )
            String nickname
    );

    @Operation(summary = "랜덤 닉네임 생성", description = "랜덤으로 사용 가능한 닉네임을 생성합니다. 형용사와 명사 조합에 랜덤 숫자를 추가하여 고유성을 보장합니다.")
    @ApiResponse(responseCode = "200", description = "랜덤 닉네임 생성 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<NicknameRandomResponse>> generateRandomNickname();

    @Operation(
            summary = "프로필 초기 설정",
            description = "온보딩 과정에서 사용자의 성별, 전화번호, 생년월일을 저장합니다."
    )
    @ApiResponse(responseCode = "204", description = "프로필 초기 설정 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.PROFILE_NOT_FOUND,
            ErrorCode.DUPLICATE_PHONE_NUMBER,
            ErrorCode.USER_ONBOARDING_NOT_FOUND
    })
    ResponseEntity<ApiResult<Void>> setupInitialProfile(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "프로필 초기 설정 요청 정보",
                    required = true
            )
            @Valid ProfileSetupRequest request
    );

    @Operation(
            summary = "프로필 수정",
            description = "닉네임, 프로필 이미지, 소개글을 수정하고 배경색을 설정합니다."
    )
    @ApiResponse(responseCode = "204", description = "프로필 이미지 및 소개글 수정 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.NICKNAME_INVALID_LENGTH,
            ErrorCode.NICKNAME_INVALID_CHARACTERS,
            ErrorCode.NICKNAME_CONTAINS_BAD_WORD,
            ErrorCode.NICKNAME_ALREADY_TAKEN,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.PROFILE_NOT_FOUND,
            ErrorCode.BG_COLOR_NOT_FOUND,
            ErrorCode.USER_ONBOARDING_NOT_FOUND
    })
    ResponseEntity<ApiResult<Void>> updateProfile(
            @Parameter(hidden = true) Long userId,
            @Valid ProfileUpdateRequest request
    );
}
