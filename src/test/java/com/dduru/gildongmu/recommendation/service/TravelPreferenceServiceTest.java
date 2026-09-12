package com.dduru.gildongmu.recommendation.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.exception.DestinationNotFoundException;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.destination.repository.DestinationRepositoryImpl;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationAvailableDate;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.dto.request.AvailableDateRequest;
import com.dduru.gildongmu.recommendation.dto.request.DestinationPreferenceRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.exception.DuplicateAvailableDateException;
import com.dduru.gildongmu.recommendation.exception.InvalidAvailableDateException;
import com.dduru.gildongmu.recommendation.exception.InvalidDestinationPreferenceException;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationAvailableDateRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QueryDslConfig.class, DestinationRepositoryImpl.class})
@DisplayName("TravelPreferenceService 테스트")
class TravelPreferenceServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private UserRecommendationDestinationPreferenceRepository destinationPreferenceRepository;

    @Autowired
    private UserRecommendationAvailableDateRepository availableDateRepository;

    private TravelPreferenceService travelPreferenceService;

    private int sequence;

    @BeforeEach
    void setUp() {
        travelPreferenceService = new TravelPreferenceService(
                destinationPreferenceRepository,
                availableDateRepository,
                userRepository,
                destinationRepository
        );
    }

    @Test
    @DisplayName("저장된 선호가 없으면 빈 목록을 반환한다")
    void getTravelPreferences_returnsEmptyWhenNoneExists() {
        User user = saveUser();

        TravelPreferenceResponse response = travelPreferenceService.getTravelPreferences(user.getId());

        assertThat(response.destinationPreferences()).isEmpty();
        assertThat(response.availableDates()).isEmpty();
    }

    @Test
    @DisplayName("COUNTRY 타입 선호는 countryName을 포함해 반환한다")
    void getTravelPreferences_returnsCountryTypeWithCountryName() {
        User user = saveUser();
        saveDestination("JP", "일본", "도쿄");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.country(user, "JP", 1));

        TravelPreferenceResponse response = travelPreferenceService.getTravelPreferences(user.getId());

        assertThat(response.destinationPreferences()).hasSize(1);
        assertThat(response.destinationPreferences().get(0).type()).isEqualTo(RecommendationDestinationPreferenceType.COUNTRY);
        assertThat(response.destinationPreferences().get(0).countryCode()).isEqualTo("JP");
        assertThat(response.destinationPreferences().get(0).countryName()).isEqualTo("일본");
    }

    @Test
    @DisplayName("CITY 타입 선호는 도시 정보를 포함해 반환한다")
    void getTravelPreferences_returnsCityTypeWithCityInfo() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.city(user, tokyo, 1));

        TravelPreferenceResponse response = travelPreferenceService.getTravelPreferences(user.getId());

        assertThat(response.destinationPreferences()).hasSize(1);
        assertThat(response.destinationPreferences().get(0).type()).isEqualTo(RecommendationDestinationPreferenceType.CITY);
        assertThat(response.destinationPreferences().get(0).destinationId()).isEqualTo(tokyo.getId());
        assertThat(response.destinationPreferences().get(0).city()).isEqualTo("도쿄");
    }

    @Test
    @DisplayName("여행 가능 날짜도 함께 반환한다")
    void getTravelPreferences_returnsAvailableDates() {
        User user = saveUser();
        availableDateRepository.save(UserRecommendationAvailableDate.of(user, date(2026, 8, 1), date(2026, 8, 7)));

        TravelPreferenceResponse response = travelPreferenceService.getTravelPreferences(user.getId());

        assertThat(response.availableDates()).hasSize(1);
        assertThat(response.availableDates().get(0).startDate()).isEqualTo(date(2026, 8, 1));
        assertThat(response.availableDates().get(0).endDate()).isEqualTo(date(2026, 8, 7));
    }

    @Test
    @DisplayName("startDate가 endDate보다 뒤면 InvalidAvailableDateException이 발생한다")
    void updateTravelPreferences_throwsWhenStartDateIsAfterEndDate() {
        User user = saveUser();
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(),
                List.of(new AvailableDateRequest(date(2026, 8, 7), date(2026, 8, 1)))
        );

        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(InvalidAvailableDateException.class);
    }

    @Test
    @DisplayName("동일한 날짜 범위가 중복되면 DuplicateAvailableDateException이 발생한다")
    void updateTravelPreferences_throwsWhenDuplicateDateRange() {
        User user = saveUser();
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(),
                List.of(
                        new AvailableDateRequest(date(2026, 8, 1), date(2026, 8, 7)),
                        new AvailableDateRequest(date(2026, 8, 1), date(2026, 8, 7))
                )
        );

        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(DuplicateAvailableDateException.class);
    }

    @Test
    @DisplayName("COUNTRY 타입에 countryCode가 없으면 InvalidDestinationPreferenceException이 발생한다")
    void updateTravelPreferences_throwsWhenCountryCodeIsMissing() {
        User user = saveUser();
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, null, null)),
                List.of()
        );

        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    @DisplayName("CITY 타입에 존재하지 않는 destinationId면 DestinationNotFoundException이 발생한다")
    void updateTravelPreferences_throwsWhenDestinationNotFound() {
        User user = saveUser();
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, 999L)),
                List.of()
        );

        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(DestinationNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 countryCode면 InvalidDestinationPreferenceException이 발생한다")
    void updateTravelPreferences_throwsWhenCountryCodeNotExistsInDB() {
        User user = saveUser();
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "XX", null)),
                List.of()
        );

        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    @DisplayName("중복 COUNTRY 요청은 하나만 저장한다")
    void updateTravelPreferences_deduplicatesCountry() {
        User user = saveUser();
        saveDestination("JP", "일본", "도쿄");
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null),
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null)
                ),
                List.of()
        );

        travelPreferenceService.updateTravelPreferences(user.getId(), request);

        assertThat(destinationPreferenceRepository.findAllByUserIdWithDestination(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("중복 CITY 요청은 하나만 저장한다")
    void updateTravelPreferences_deduplicatesCity() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, tokyo.getId()),
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, tokyo.getId())
                ),
                List.of()
        );

        travelPreferenceService.updateTravelPreferences(user.getId(), request);

        assertThat(destinationPreferenceRepository.findAllByUserIdWithDestination(user.getId())).hasSize(1);
    }

    @Test
    @DisplayName("COUNTRY와 같은 국가의 CITY는 함께 저장된다")
    void updateTravelPreferences_allowsCountryAndCityOfSameCountry() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        Destination osaka = saveDestination("JP", "일본", "오사카");
        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null),
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, tokyo.getId()),
                        new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, osaka.getId())
                ),
                List.of()
        );

        travelPreferenceService.updateTravelPreferences(user.getId(), request);

        assertThat(destinationPreferenceRepository.findAllByUserIdWithDestination(user.getId())).hasSize(3);
    }

    @Test
    @DisplayName("업데이트 시 기존 선호를 삭제하고 새로운 선호로 교체한다")
    void updateTravelPreferences_replacesExistingPreferences() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        Destination busan = saveDestination("KR", "대한민국", "부산");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.city(user, tokyo, 1));
        availableDateRepository.save(UserRecommendationAvailableDate.of(user, date(2026, 7, 1), date(2026, 7, 7)));

        TravelPreferenceUpdateRequest request = new TravelPreferenceUpdateRequest(
                List.of(new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, busan.getId())),
                List.of(new AvailableDateRequest(date(2026, 8, 1), date(2026, 8, 7)))
        );

        travelPreferenceService.updateTravelPreferences(user.getId(), request);

        List<UserRecommendationDestinationPreference> prefs = destinationPreferenceRepository.findAllByUserIdWithDestination(user.getId());
        assertThat(prefs).hasSize(1);
        assertThat(prefs.get(0).getDestination().getCity()).isEqualTo("부산");

        List<UserRecommendationAvailableDate> dates = availableDateRepository.findAllByUser_Id(user.getId());
        assertThat(dates).hasSize(1);
        assertThat(dates.get(0).getStartDate()).isEqualTo(date(2026, 8, 1));
    }

    @Test
    void ranksFollowDeduplicatedOrderAndCanBeReplacedAndCleared() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        var city = new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, tokyo.getId());
        var country = new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null);
        travelPreferenceService.updateTravelPreferences(user.getId(),
                new TravelPreferenceUpdateRequest(List.of(city, city, country), List.of()));
        var first = travelPreferenceService.getTravelPreferences(user.getId()).destinationPreferences();
        assertThat(first).extracting("preferenceRank").containsExactly(1, 2);
        assertThat(first).extracting("type").containsExactly(RecommendationDestinationPreferenceType.CITY,
                RecommendationDestinationPreferenceType.COUNTRY);
        travelPreferenceService.updateTravelPreferences(user.getId(),
                new TravelPreferenceUpdateRequest(List.of(country, city), List.of()));
        assertThat(destinationPreferenceRepository.findFirstPreferenceByUserId(user.getId()).orElseThrow().getCountryCode())
                .isEqualTo("JP");
        assertThat(destinationPreferenceRepository.findFilterRowsByUserId(user.getId())).hasSize(2);
        travelPreferenceService.updateTravelPreferences(user.getId(),
                new TravelPreferenceUpdateRequest(List.of(city), List.of()));
        assertThat(travelPreferenceService.getTravelPreferences(user.getId()).destinationPreferences())
                .extracting("preferenceRank").containsExactly(1);
        travelPreferenceService.updateTravelPreferences(user.getId(),
                new TravelPreferenceUpdateRequest(List.of(), List.of()));
        assertThat(destinationPreferenceRepository.existsByUser_Id(user.getId())).isFalse();
    }

    @Test
    void rejectsMoreThanThreeBeforeDeduplication() {
        User user = saveUser();
        var country = new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null);
        var request = new TravelPreferenceUpdateRequest(List.of(country, country, country, country), List.of());
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isNotEmpty();
            assertThat(factory.getValidator().validate(new TravelPreferenceUpdateRequest(List.of(), List.of()))).isEmpty();
        }
        assertThatThrownBy(() -> travelPreferenceService.updateTravelPreferences(user.getId(), request))
                .isInstanceOf(InvalidDestinationPreferenceException.class);
    }

    @Test
    void rankOrderIsIndependentOfInsertionOrder() {
        User user = saveUser();
        Destination tokyo = saveDestination("JP", "일본", "도쿄");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.city(user, tokyo, 2));
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.country(user, "JP", 1));
        assertThat(travelPreferenceService.getTravelPreferences(user.getId()).destinationPreferences())
                .extracting("preferenceRank").containsExactly(1, 2);
    }

    private User saveUser() {
        sequence++;
        return userRepository.save(User.builder()
                .email("user" + sequence + "@example.com")
                .name("사용자" + sequence)
                .oauthId("oauth-" + sequence)
                .oauthType(OauthType.KAKAO)
                .build());
    }

    private Destination saveDestination(String countryCode, String countryName, String city) {
        return destinationRepository.save(Destination.builder()
                .countryCode(countryCode)
                .countryName(countryName)
                .city(city)
                .image("https://example.com/" + city + ".jpg")
                .build());
    }

    private LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }
}
