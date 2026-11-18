package com.dduru.gildongmu.block.dto.response;

import com.dduru.gildongmu.profile.dto.response.ProfileImageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차단 사용자 요약")
public record BlockedUserResponse(
        @Schema(description = "사용자 ID", example = "12") Long userId,
        @Schema(description = "닉네임", example = "여행자") String nickname,
        @Schema(description = "한 줄 소개", nullable = true, example = "맛집 탐방을 좋아해요") String bio,
        @Schema(description = "프로필 이미지") ProfileImageInfo profileImage
) {
}
