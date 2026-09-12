package com.dduru.gildongmu.recommendation.domain;

import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.exception.InvalidDestinationPreferenceException;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserRecommendationDestinationPreference 테스트")
class UserRecommendationDestinationPreferenceTest {

    @Test
    @DisplayName("COUNTRY 선호는 두 자리 국가 코드로 생성한다")
    void createsCountryPreference() {
        UserRecommendationDestinationPreference preference =
                UserRecommendationDestinationPreference.country(user(), "KR", 1);

        assertThat(preference.getPreferenceType()).isEqualTo(RecommendationDestinationPreferenceType.COUNTRY);
        assertThat(preference.getCountryCode()).isEqualTo("KR");
        assertThat(preference.getDestination()).isNull();
    }

    @Test
    @DisplayName("COUNTRY 선호는 국가 코드 없이 생성할 수 없다")
    void cannotCreateCountryPreferenceWithoutCountryCode() {
        assertThatThrownBy(() -> UserRecommendationDestinationPreference.country(user(), null, 1))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
        assertThatThrownBy(() -> UserRecommendationDestinationPreference.country(user(), " ", 1))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    @DisplayName("COUNTRY 선호는 두 자리가 아닌 국가 코드로 생성할 수 없다")
    void cannotCreateCountryPreferenceWithInvalidCountryCodeLength() {
        assertThatThrownBy(() -> UserRecommendationDestinationPreference.country(user(), "KOR", 1))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    @DisplayName("CITY 선호는 여행지로 생성한다")
    void createsCityPreference() {
        Destination destination = Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("서울")
                .build();

        UserRecommendationDestinationPreference preference =
                UserRecommendationDestinationPreference.city(user(), destination, 1);

        assertThat(preference.getPreferenceType()).isEqualTo(RecommendationDestinationPreferenceType.CITY);
        assertThat(preference.getCountryCode()).isNull();
        assertThat(preference.getDestination()).isSameAs(destination);
    }

    @Test
    @DisplayName("CITY 선호는 여행지 없이 생성할 수 없다")
    void cannotCreateCityPreferenceWithoutDestination() {
        assertThatThrownBy(() -> UserRecommendationDestinationPreference.city(user(), null, 1))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    void invalidRankIsRejected() {
        for (int rank : new int[]{0, 4}) {
            assertThatThrownBy(() -> UserRecommendationDestinationPreference.country(user(), "KR", rank))
                    .isInstanceOf(InvalidDestinationPreferenceException.class);
        }
    }

    private User user() {
        return User.builder()
                .email("destination-preference@example.com")
                .name("destination-preference")
                .oauthId("destination-preference-oauth")
                .oauthType(OauthType.KAKAO)
                .build();
    }
}
