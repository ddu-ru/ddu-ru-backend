package com.dduru.gildongmu.profile.service;

import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.dto.response.MyProfileResponse;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.response.AvailableDateResponse;
import com.dduru.gildongmu.recommendation.dto.response.DestinationPreferenceResponse;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import com.dduru.gildongmu.survey.domain.AvatarProfile;
import com.dduru.gildongmu.survey.domain.enums.AvatarType;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileQueryService 테스트")
class ProfileQueryServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileImageResolver profileImageResolver;
    @Mock private TravelPreferenceService travelPreferenceService;
    @Mock private JsonConverter jsonConverter;

    @InjectMocks
    private ProfileQueryService profileQueryService;

    private User user;
    private Profile profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("테스트")
                .oauthId("12345")
                .oauthType(OauthType.KAKAO)
                .build();
        profile = new Profile(user);
        ReflectionTestUtils.setField(profile, "nickname", "김해나");
        ReflectionTestUtils.setField(profile, "bio", "맛집 찾는 여행 좋아해요");
        lenient().when(travelPreferenceService.getTravelPreferences(1L))
                .thenReturn(new TravelPreferenceResponse(List.of(), List.of()));
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("닉네임과 한줄소개를 반환한다")
        void returnsNicknameAndBio() {
            when(profileRepository.getByUserIdOrThrow(1L)).thenReturn(profile);

            MyProfileResponse response = profileQueryService.getMyProfile(1L);

            assertThat(response.nickname()).isEqualTo("김해나");
            assertThat(response.bio()).isEqualTo("맛집 찾는 여행 좋아해요");
        }

        @Test
        @DisplayName("bio가 없으면 null을 반환한다")
        void returnsNullBioWhenNotSet() {
            ReflectionTestUtils.setField(profile, "bio", null);
            when(profileRepository.getByUserIdOrThrow(1L)).thenReturn(profile);

            MyProfileResponse response = profileQueryService.getMyProfile(1L);

            assertThat(response.bio()).isNull();
        }

        @Test
        @DisplayName("선호 여행지와 여행 가능 날짜를 함께 반환한다")
        void returnsTravelPreferences() {
            TravelPreferenceResponse travelPreferences = new TravelPreferenceResponse(
                    List.of(new DestinationPreferenceResponse(
                            com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType.CITY,
                            "JP", "일본", 1L, "도쿄", 1
                    )),
                    List.of(new AvailableDateResponse(
                            LocalDate.of(2026, 3, 15),
                            LocalDate.of(2026, 3, 20)
                    ))
            );
            when(profileRepository.getByUserIdOrThrow(1L)).thenReturn(profile);
            when(travelPreferenceService.getTravelPreferences(1L)).thenReturn(travelPreferences);

            MyProfileResponse response = profileQueryService.getMyProfile(1L);

            assertThat(response.travelPreferences()).isEqualTo(travelPreferences);
        }

        @Test
        @DisplayName("아바타 성향의 이름과 한줄 설명과 태그를 여행 스타일 요약으로 반환한다")
        void returnsTravelStyleSummary() {
            AvatarProfile avatarProfile = AvatarProfile.builder()
                    .avatarType(AvatarType.TTUR_SWEET)
                    .displayName("스윗")
                    .speechBubbleText("좋은 건 무조건 다 경험해봐야죠!")
                    .descriptionLine1("설명1")
                    .descriptionLine2("설명2")
                    .descriptionLine3("설명3")
                    .imageUrl("https://example.com/sweet.png")
                    .tags("[\"열정적\",\"도전정신\",\"적극적\"]")
                    .build();
            profile.updateAvatar(avatarProfile);
            when(profileRepository.getByUserIdOrThrow(1L)).thenReturn(profile);
            when(jsonConverter.convertJsonToList(avatarProfile.getTags()))
                    .thenReturn(List.of("열정적", "도전정신", "적극적"));

            MyProfileResponse response = profileQueryService.getMyProfile(1L);

            assertThat(response.travelStyleSummary().name()).isEqualTo("스윗");
            assertThat(response.travelStyleSummary().description())
                    .isEqualTo("좋은 건 무조건 다 경험해봐야죠!");
            assertThat(response.travelStyleSummary().tags())
                    .containsExactly("열정적", "도전정신", "적극적");
        }

        @Test
        @DisplayName("설문을 완료하지 않아 아바타가 없으면 여행 스타일 요약은 null이다")
        void returnsNullTravelStyleSummaryWithoutAvatar() {
            when(profileRepository.getByUserIdOrThrow(1L)).thenReturn(profile);

            MyProfileResponse response = profileQueryService.getMyProfile(1L);

            assertThat(response.travelStyleSummary()).isNull();
        }
    }
}
