package com.dduru.gildongmu.profile.dto.response;

import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "마이페이지 프로필 조회 응답")
public record MyProfileResponse(
        @Schema(description = "닉네임", example = "김해나")
        String nickname,

        @Schema(description = "한줄소개. 미설정 시 null", example = "맛집 찾는 여행 좋아해요", nullable = true)
        String bio,

        @Schema(description = "프로필 이미지 정보")
        ProfileImageInfo profileImage,

        @Schema(description = "성별. 미설정 시 null", example = "F", allowableValues = {"M", "F"}, nullable = true)
        Gender gender,

        @Schema(description = "생년월일. 미설정 시 null", example = "1998-05-10", nullable = true)
        LocalDate birthday,

        @Schema(description = "전화번호. 미설정 시 null", example = "01012345678", nullable = true)
        String phoneNumber
) {
    public static MyProfileResponse of(Profile profile, ProfileImageResolver profileImageResolver) {
        return new MyProfileResponse(
                profile.getNickname(),
                profile.getBio(),
                ProfileImageInfo.from(profile, profileImageResolver),
                profile.getGender(),
                profile.getBirthday(),
                profile.getPhoneNumber()
        );
    }
}
