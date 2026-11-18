package com.dduru.gildongmu.user.dto.response;

import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.dto.response.ProfileImageInfo;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 홈 요약 응답")
public record MyPageOverviewResponse(
        @Schema(description = "닉네임", example = "차한잔마시고해")
        String nickname,
        @Schema(description = "한줄소개. 미설정 시 null", nullable = true)
        String bio,
        @Schema(description = "프로필 이미지 정보")
        ProfileImageInfo profileImage,
        @Schema(description = "선호 여행지와 여행 가능 날짜")
        TravelPreferenceResponse travelPreferences,
        @Schema(description = "여행 스타일 요약. 설문 미완료 시 null", nullable = true)
        MyPageTravelStyleSummaryResponse travelStyleSummary
) {
    public static MyPageOverviewResponse of(
            Profile profile,
            ProfileImageResolver profileImageResolver,
            TravelPreferenceResponse travelPreferences,
            MyPageTravelStyleSummaryResponse travelStyleSummary
    ) {
        return new MyPageOverviewResponse(
                profile.getNickname(),
                profile.getBio(),
                ProfileImageInfo.from(profile, profileImageResolver),
                travelPreferences,
                travelStyleSummary
        );
    }
}
