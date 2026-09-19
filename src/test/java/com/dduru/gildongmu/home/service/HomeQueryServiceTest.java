package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.common.time.KoreaTime;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.dto.response.HomeSuperHostResponse;
import com.dduru.gildongmu.home.dto.response.MateRecommendationResponse;
import com.dduru.gildongmu.home.enums.UserAccessStatus;
import com.dduru.gildongmu.home.mapper.HomeRecommendationMapper;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.exception.CurrentOrUpcomingJourneyNotFoundException;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.journey.repository.JourneyScheduleRepository;
import com.dduru.gildongmu.onboarding.domain.UserOnboarding;
import com.dduru.gildongmu.onboarding.repository.UserOnboardingRepository;
import com.dduru.gildongmu.onboarding.service.OnboardingService;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.query.MateRecommendationCardQueryResult;
import com.dduru.gildongmu.recommendation.dto.result.MateRecommendationQueryResult;
import com.dduru.gildongmu.recommendation.service.DailyMateRecommendationQueryService;
import com.dduru.gildongmu.recommendation.support.RecommendationReasonJsonConverter;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Home 조회 서비스 테스트")
class HomeQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 13, 12, 30);

    private UserOnboardingRepository userOnboardingRepository;
    private JourneyRepository journeyRepository;
    private JourneyScheduleRepository journeyScheduleRepository;
    private DailyMateRecommendationQueryService dailyMateRecommendationQueryService;
    private HomeOverviewQueryService overviewQueryService;
    private HomePopularDestinationQueryService popularDestinationQueryService;
    private HomeTripQueryService tripQueryService;
    private HomeRecommendationQueryService recommendationQueryService;
    private HomeSuperHostQueryService superHostQueryService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = new TimeProvider(Clock.fixed(
                NOW.atZone(KoreaTime.ZONE_ID).toInstant(),
                KoreaTime.ZONE_ID
        ));
        userOnboardingRepository = mock(UserOnboardingRepository.class);
        journeyRepository = mock(JourneyRepository.class, CALLS_REAL_METHODS);
        journeyScheduleRepository = mock(JourneyScheduleRepository.class);
        OnboardingService onboardingService = new OnboardingService(userOnboardingRepository);
        dailyMateRecommendationQueryService = mock(DailyMateRecommendationQueryService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        overviewQueryService = new HomeOverviewQueryService(onboardingService);
        popularDestinationQueryService = new HomePopularDestinationQueryService(timeProvider);
        tripQueryService = new HomeTripQueryService(
                timeProvider,
                onboardingService,
                journeyScheduleRepository,
                journeyRepository
        );
        recommendationQueryService = new HomeRecommendationQueryService(
                dailyMateRecommendationQueryService,
                new HomeRecommendationMapper(
                        new RecommendationReasonJsonConverter(objectMapper),
                        mock(ProfileImageResolver.class),
                        new JsonConverter(objectMapper)
                )
        );
        superHostQueryService = new HomeSuperHostQueryService(timeProvider);
    }

    @Nested
    @DisplayName("홈 초기 구성")
    class Overview {

        @Test
        @DisplayName("비회원은 온보딩을 조회하지 않고 GUEST 상태를 받는다")
        void guest() {
            assertThat(overviewQueryService.retrieve(null).userAccessStatus())
                    .isEqualTo(UserAccessStatus.GUEST);
            verifyNoInteractions(userOnboardingRepository);
        }

        @Test
        @DisplayName("설문 완료 회원은 완료 상태를 받는다")
        void surveyCompletedMember() {
            when(userOnboardingRepository.getByUserIdOrThrow(10L)).thenReturn(onboarding(true));

            assertThat(overviewQueryService.retrieve(10L).userAccessStatus())
                    .isEqualTo(UserAccessStatus.MEMBER_SURVEY_COMPLETED);
        }
    }

    @Nested
    @DisplayName("인기 여행지")
    class PopularDestinations {

        @Test
        @DisplayName("mock 인기 여행지 5개와 태그를 반환한다")
        void retrieve() {
            HomePopularDestinationResponse response = popularDestinationQueryService.retrieve();

            assertThat(response.updatedAt()).isEqualTo(NOW);
            assertThat(response.items()).hasSize(5);
            assertThat(response.items().get(0).destinationName()).isEqualTo("제주도");
            assertThat(response.items().get(0).tags()).containsExactly("힐링", "드라이브", "바다");
            assertThat(response.items().get(3).tags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원 여행")
    class MemberTrips {

        @Test
        @DisplayName("진행 중이거나 예정된 나의 여정이 없으면 조회할 수 없다")
        void journeyRequired() {
            when(journeyRepository.findCurrentAndUpcomingJourneys(
                    eq(10L), eq(NOW.toLocalDate()), org.mockito.ArgumentMatchers.any(Pageable.class)
            )).thenReturn(List.of());

            assertThatThrownBy(() -> tripQueryService.retrieveUpcomingTrip(10L))
                    .isInstanceOf(CurrentOrUpcomingJourneyNotFoundException.class)
                    .hasMessage(ErrorCode.CURRENT_OR_UPCOMING_JOURNEY_NOT_FOUND.getMessage());
            verifyNoInteractions(journeyScheduleRepository);
        }

        @Test
        @DisplayName("진행 중이거나 예정된 여정 중 시작일이 가장 빠른 한 건을 조회한다")
        void nearestCurrentOrUpcomingJourney() {
            LocalDate startDate = NOW.toLocalDate().minusDays(1);
            LocalDate endDate = NOW.toLocalDate().plusDays(2);
            Journey journey = mock(Journey.class);
            Post post = mock(Post.class);
            when(journey.getId()).thenReturn(102L);
            when(journey.getTitle()).thenReturn("제주도 힐링 여행");
            when(journey.getPost()).thenReturn(post);
            when(post.getStartDate()).thenReturn(startDate);
            when(post.getEndDate()).thenReturn(endDate);
            when(post.getRecruitCount()).thenReturn(3);
            when(post.getRecruitCapacity()).thenReturn(4);
            when(journeyScheduleRepository.countByJourneyIdAndIsDeletedFalse(102L)).thenReturn(3);
            when(journeyRepository.findCurrentAndUpcomingJourneys(
                    eq(10L), eq(NOW.toLocalDate()), org.mockito.ArgumentMatchers.any(Pageable.class)
            )).thenReturn(List.of(journey));

            var response = tripQueryService.retrieveUpcomingTrip(10L);

            assertThat(response.journeyId()).isEqualTo(102L);
            assertThat(response.dDay()).isZero();
            assertThat(response.startDate()).isEqualTo(startDate);
            assertThat(response.endDate()).isEqualTo(endDate);
            assertThat(response.scheduleCount()).isEqualTo(3);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(journeyRepository).findCurrentAndUpcomingJourneys(
                    eq(10L), eq(NOW.toLocalDate()), pageableCaptor.capture()
            );
            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
            assertThat(pageableCaptor.getValue().getPageSize()).isOne();
            verify(journeyScheduleRepository).countByJourneyIdAndIsDeletedFalse(102L);
        }

        @Test
        @DisplayName("온보딩 정보가 있으면 같은 여행지 여행을 조회한다")
        void sameDestinationTrips() {
            when(userOnboardingRepository.getByUserIdOrThrow(10L)).thenReturn(onboarding(false));

            assertThat(tripQueryService.retrieveSameDestinationTrips(10L)).hasSize(3);
        }
    }

    @Nested
    @DisplayName("메이트 추천")
    class MateRecommendations {

        @Test
        @DisplayName("설문 미완료 상태를 홈 응답으로 변환한다")
        void surveyRequired() {
            when(dailyMateRecommendationQueryService.retrieve(10L))
                    .thenReturn(MateRecommendationQueryResult.surveyRequired());

            assertThat(recommendationQueryService.retrieve(10L).availabilityStatus())
                    .isEqualTo(MateRecommendationResponse.AvailabilityStatus.SURVEY_REQUIRED);
        }

        @Test
        @DisplayName("추천 생성 중 상태를 홈 응답으로 변환한다")
        void generating() {
            when(dailyMateRecommendationQueryService.retrieve(10L))
                    .thenReturn(MateRecommendationQueryResult.generating());

            assertThat(recommendationQueryService.retrieve(10L).availabilityStatus())
                    .isEqualTo(MateRecommendationResponse.AvailabilityStatus.GENERATING);
        }

        @Test
        @DisplayName("추천 결과와 추천 이유를 홈 카드 응답으로 변환한다")
        void recommendationCard() {
            when(dailyMateRecommendationQueryService.retrieve(10L)).thenReturn(
                    MateRecommendationQueryResult.available(NOW.toLocalDate(), List.of(sampleRecommendationCard()))
            );

            MateRecommendationResponse response = recommendationQueryService.retrieve(10L);

            assertThat(response.availabilityStatus())
                    .isEqualTo(MateRecommendationResponse.AvailabilityStatus.AVAILABLE);
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.recommendations().get(0).thumbnailUrl()).isEqualTo("https://example.com/trips/jeju.jpg");
            assertThat(response.recommendations().get(0).matchReasons())
                    .extracting("code", "message")
                    .containsExactly(tuple("RHYTHM_MATCH", "여행 리듬이 잘 맞아요"));
        }
    }

    @Test
    @DisplayName("mock 슈퍼호스트 5개를 반환하고 hasLiked는 false다")
    void superHosts() {
        List<HomeSuperHostResponse> response = superHostQueryService.retrieve(10L);

        assertThat(response).hasSize(5);
        assertThat(response.get(0).postId()).isEqualTo(501L);
        assertThat(response.get(0).tags()).containsExactly("일출", "등산");
        assertThat(response.get(0).hasLiked()).isFalse();
    }

    private MateRecommendationCardQueryResult sampleRecommendationCard() {
        return new MateRecommendationCardQueryResult(
                11L,
                101L,
                1,
                92,
                "[{\"code\":\"RHYTHM_MATCH\",\"message\":\"여행 리듬이 잘 맞아요\"}]",
                "[]",
                "제주 여행 동행 모집",
                "https://example.com/trips/jeju.jpg",
                "대한민국",
                "제주",
                NOW.toLocalDate().plusDays(5),
                NOW.toLocalDate().plusDays(7),
                CompanionType.FULL,
                2,
                4,
                "홈 추천 카드 응답을 검증하기 위한 충분한 길이의 본문입니다.",
                "[\"힐링\"]",
                "제주호스트",
                ProfileImageType.DEFAULT,
                null,
                null,
                null,
                NOW.toLocalDate().minusYears(30),
                Gender.M
        );
    }

    private UserOnboarding onboarding(boolean surveyCompleted) {
        UserOnboarding onboarding = new UserOnboarding(user());
        if (surveyCompleted) {
            onboarding.completeSurvey();
        }
        return onboarding;
    }

    private User user() {
        return User.builder()
                .email("home-service@example.com")
                .name("홈서비스유저")
                .oauthId("home-service-oauth-id")
                .oauthType(OauthType.KAKAO)
                .build();
    }
}
