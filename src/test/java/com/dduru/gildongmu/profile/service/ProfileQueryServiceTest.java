package com.dduru.gildongmu.profile.service;

import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.dto.response.MyProfileResponse;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileQueryService 테스트")
class ProfileQueryServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileImageResolver profileImageResolver;

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

    }
}
