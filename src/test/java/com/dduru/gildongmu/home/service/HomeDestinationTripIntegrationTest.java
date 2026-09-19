package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.home.repository.HomeTripQueryRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.dto.request.DestinationPreferenceRequest;
import com.dduru.gildongmu.recommendation.dto.request.AvailableDateRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({QueryDslConfig.class, HomeTripQueryRepository.class,
        HomeTripQueryService.class, TravelPreferenceService.class})
class HomeDestinationTripIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 12);
    @Autowired EntityManager em;
    @Autowired HomeTripQueryService service;
    @Autowired TravelPreferenceService preferences;
    @Autowired UserRecommendationDestinationPreferenceRepository preferenceRepository;
    @MockitoBean TimeProvider timeProvider;
    private User viewer;
    private User host;
    private Destination tokyo;
    private Destination osaka;
    private Destination busan;

    @BeforeEach
    void setUp() {
        when(timeProvider.today()).thenReturn(TODAY);
        viewer = user("viewer");
        host = user("host");
        tokyo = destination("JP", "일본", "도쿄");
        osaka = destination("JP", "일본", "오사카");
        busan = destination("KR", "대한민국", "부산");
    }

    @Test
    void randomThreeFromOldestThirtyOnlyFromFirstCityAndRankChangeAppliesImmediately() {
        var posts = IntStream.range(0, 31).mapToObj(i -> post(host, tokyo)).toList();
        var secondCity = post(host, busan);
        setPreferences(tokyo, busan);
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).extracting("postId")
                .hasSize(3)
                .allMatch(postId -> posts.subList(0, 30).stream().map(Post::getId).toList().contains(postId));
        setPreferences(busan, tokyo);
        var result = service.retrieveSameDestinationTrips(viewer.getId());
        assertThat(result).extracting("postId").containsExactly(secondCity.getId());
        assertThat(result.get(0).location()).isEqualTo("부산");
        assertThat(result.get(0).thumbnailUrl()).isNull();
        assertThat(result.get(0).currentMemberCount()).isEqualTo(1);
        assertThat(result.get(0).maxMemberCount()).isEqualTo(4);
    }

    @Test
    void countryPreferenceIncludesAllItsCitiesButNotOtherCountries() {
        var first = post(host, tokyo);
        var second = post(host, osaka);
        post(host, busan);
        em.persist(UserRecommendationDestinationPreference.country(viewer, "JP", 1));
        em.persist(UserRecommendationDestinationPreference.city(viewer, busan, 2));
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).extracting("postId")
                .containsExactlyInAnyOrder(second.getId(), first.getId());
        assertThat(preferenceRepository.findFilterRowsByUserId(viewer.getId())).hasSize(2);
    }

    @Test
    void emptyWithoutPreferencesAndNoFallbackWhenFirstCityHasNoPosts() {
        post(host, busan);
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).isEmpty();
        setPreferences(tokyo, busan);
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).isEmpty();
    }

    @Test
    void usesOpenStatusForRecruitmentAvailabilityAndIncludesTodayBoundariesAndOngoingTrips() {
        var eligible = post(host, tokyo);
        change(eligible, "start_date = '2026-09-11', end_date = '2026-09-12', recruit_deadline = '2026-09-12'");
        change(post(host, tokyo), "is_deleted = true");
        change(post(host, tokyo), "status = 'CLOSED'");
        change(post(host, tokyo), "recruit_count = recruit_capacity");
        change(post(host, tokyo), "recruit_count = recruit_capacity + 1");
        change(post(host, tokyo), "end_date = '2026-09-11'");
        var expiredDeadlineButOpen = post(host, tokyo);
        change(expiredDeadlineButOpen, "recruit_deadline = '2026-09-11'");
        post(viewer, tokyo);
        var reported = post(host, tokyo);
        em.persist(Report.createReport(viewer, reported, ReportReason.values()[0], "신고 사유"));
        setPreferences(tokyo);
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).extracting("postId")
                .containsExactlyInAnyOrder(expiredDeadlineButOpen.getId(), eligible.getId());
    }

    @Test
    void doesNotApplyGenderOrAgeEligibility() {
        var post = post(host, tokyo);
        change(post, "preferred_gender = 'F', is_age_any = false, min_age = 50, max_age = 60");
        setPreferences(tokyo);
        assertThat(service.retrieveSameDestinationTrips(viewer.getId())).extracting("postId").containsExactly(post.getId());
    }

    private void setPreferences(Destination... destinations) {
        preferences.updateTravelPreferences(viewer.getId(), testRequest(
                java.util.Arrays.stream(destinations).map(d -> new DestinationPreferenceRequest(
                        RecommendationDestinationPreferenceType.CITY, null, d.getId())).toList(), List.of()));
    }

    private void change(Post post, String assignments) {
        em.flush();
        em.createNativeQuery("UPDATE posts SET " + assignments + " WHERE id = :id")
                .setParameter("id", post.getId()).executeUpdate();
        em.clear();
    }

    private Post post(User author, Destination destination) {
        Post post = Post.createPost(author, destination, "여행 동행을 모집합니다", "함께 여행할 동행을 모집하는 테스트 게시글입니다.",
                TODAY.plusDays(1), TODAY.plusDays(3), 4, null, Gender.U, true,
                null, null, null, "[]", CompanionType.FULL);
        em.persist(post);
        return post;
    }

    private User user(String name) {
        User user = User.builder().email(name + "@example.com").name(name)
                .oauthId(name).oauthType(OauthType.KAKAO).build();
        em.persist(user);
        return user;
    }

    private Destination destination(String code, String country, String city) {
        Destination destination = Destination.builder().countryCode(code).countryName(country)
                .city(city).image("https://example.com/image.jpg").build();
        em.persist(destination);
        return destination;
    }
    private TravelPreferenceUpdateRequest testRequest(
            List<DestinationPreferenceRequest> destinationPreferences,
            List<AvailableDateRequest> availableDates
    ) {
        return new TravelPreferenceUpdateRequest(destinationPreferences, availableDates);
    }

}
