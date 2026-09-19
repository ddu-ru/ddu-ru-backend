package com.dduru.gildongmu.recommendation.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.home.dto.response.MateRecommendationResponse;
import com.dduru.gildongmu.home.mapper.HomeRecommendationMapper;
import com.dduru.gildongmu.home.service.HomeRecommendationQueryService;
import com.dduru.gildongmu.onboarding.domain.UserOnboarding;
import com.dduru.gildongmu.onboarding.repository.UserOnboardingRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.exception.ProfileNotFoundException;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.domain.MateRecommendationBatch;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationAvailableDate;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.MateRecommendationBatchStatus;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationAvailabilityStatus;
import com.dduru.gildongmu.recommendation.dto.query.MateRecommendationCardQueryResult;
import com.dduru.gildongmu.recommendation.dto.result.MateRecommendationQueryResult;
import com.dduru.gildongmu.recommendation.exception.RecommendationTendencyMissingException;
import com.dduru.gildongmu.recommendation.repository.ApplicantRecommendationQueryRepository;
import com.dduru.gildongmu.recommendation.repository.MateRecommendationBatchRepository;
import com.dduru.gildongmu.recommendation.repository.MateRecommendationCardQueryRepository;
import com.dduru.gildongmu.recommendation.repository.MateRecommendationRepository;
import com.dduru.gildongmu.recommendation.repository.RecommendablePostQueryRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationAvailableDateRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.recommendation.support.RecommendationAvailableDateMatcher;
import com.dduru.gildongmu.recommendation.support.RecommendationReasonJsonConverter;
import com.dduru.gildongmu.recommendation.support.RecommendationScoreCalculator;
import com.dduru.gildongmu.survey.domain.TravelTendency;
import com.dduru.gildongmu.survey.domain.enums.AvatarType;
import com.dduru.gildongmu.survey.repository.TravelTendencyRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        QueryDslConfig.class,
        HomeRecommendationQueryService.class,
        HomeRecommendationMapper.class,
        ProfileImageResolver.class,
        ApplicantRecommendationQueryRepository.class,
        RecommendablePostQueryRepository.class,
        MateRecommendationCardQueryRepository.class,
        RecommendationApplicantContextResolver.class,
        PostRecommendationSelectionService.class,
        RecommendationBatchClaimService.class,
        RecommendationBatchCompletionService.class,
        RecommendationBatchFailureService.class,
        DailyMateRecommendationService.class,
        VisibleMateRecommendationCardQueryService.class,
        DailyMateRecommendationQueryService.class,
        MateRecommendationFlowIntegrationTest.TestConfig.class
})
@DisplayName("메이트 추천 실제 데이터 통합 테스트")
class MateRecommendationFlowIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 29);

    @Autowired private DailyMateRecommendationQueryService queryService;
    @Autowired private HomeRecommendationQueryService homeService;
    @MockitoBean private TimeProvider timeProvider;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void fixDate() {
        when(timeProvider.today()).thenReturn(TODAY);
        // H2 JSON은 JDBC 문자열을 JSON 문자열로 감싼다. MySQL에서 조회되는 원문 JSON을
        // 동일하게 매핑하도록 이 테스트 DB의 문자열 기반 JSON 컬럼만 VARCHAR로 사용한다.
        jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN tags VARCHAR(10000)");
        jdbcTemplate.execute("ALTER TABLE mate_recommendations ALTER COLUMN match_reasons VARCHAR(10000)");
        jdbcTemplate.execute("ALTER TABLE mate_recommendations ALTER COLUMN caution_points VARCHAR(10000)");
    }
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private UserOnboardingRepository onboardingRepository;
    @Autowired private TravelTendencyRepository tendencyRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRecommendationDestinationPreferenceRepository destinationPreferenceRepository;
    @Autowired private UserRecommendationAvailableDateRepository availableDateRepository;
    @Autowired private MateRecommendationBatchRepository batchRepository;
    @Autowired private MateRecommendationRepository recommendationRepository;

    @AfterEach
    void tearDown() {
        recommendationRepository.deleteAllInBatch();
        batchRepository.deleteAllInBatch();
        destinationPreferenceRepository.deleteAllInBatch();
        availableDateRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        tendencyRepository.deleteAllInBatch();
        onboardingRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        destinationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("DB에 저장한 사용자 조건과 여행방으로 일일 추천을 생성하고 실제 카드 데이터를 반환한다")
    void createsAndReturnsRecommendationsFromPersistedData() {
        User applicant = user("applicant");
        profile(applicant, "신청자", Gender.F, LocalDate.of(2000, 8, 29));
        completeSurvey(applicant);
        tendency(applicant, 5, 5, 5, 5);

        Destination jeju = destination("KR", "대한민국", "제주");
        Destination busan = destination("KR", "대한민국", "부산");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.city(applicant, jeju));
        availableDateRepository.save(UserRecommendationAvailableDate.of(
                applicant,
                TODAY.plusDays(5),
                TODAY.plusDays(10)
        ));

        User bestHost = host("best-host", "제주호스트", 5, 5, 5, 5);
        User secondHost = host("second-host", "차순위호스트", 7, 7, 7, 7);
        User excludedHost = host("excluded-host", "부산호스트", 5, 5, 5, 5);
        Post bestPost = post(bestHost, jeju, "제주 완벽 매칭 여행", TODAY.plusDays(5), TODAY.plusDays(7));
        Post secondPost = post(secondHost, jeju, "제주 성향 매칭 여행", TODAY.plusDays(6), TODAY.plusDays(8));
        post(excludedHost, busan, "부산 제외 대상 여행", TODAY.plusDays(5), TODAY.plusDays(7));

        MateRecommendationQueryResult first = queryService.retrieve(applicant.getId());

        assertThat(first.availabilityStatus()).isEqualTo(RecommendationAvailabilityStatus.AVAILABLE);
        assertThat(first.referenceDate()).isEqualTo(TODAY);
        assertThat(first.recommendations())
                .extracting(
                        MateRecommendationCardQueryResult::postId,
                        MateRecommendationCardQueryResult::recommendationRank,
                        MateRecommendationCardQueryResult::matchPercentage,
                        MateRecommendationCardQueryResult::title,
                        MateRecommendationCardQueryResult::city,
                        MateRecommendationCardQueryResult::hostNickname
                )
                .containsExactly(
                        tuple(bestPost.getId(), 1, 100, "제주 완벽 매칭 여행", "제주", "제주호스트"),
                        tuple(secondPost.getId(), 2, 80, "제주 성향 매칭 여행", "제주", "차순위호스트")
                );
        assertThat(first.recommendations().get(0).matchReasons()).contains("RHYTHM_MATCH");

        MateRecommendationBatch batch = batchRepository
                .findByUser_IdAndRecommendationDate(applicant.getId(), TODAY)
                .orElseThrow();
        assertThat(batch.getStatus()).isEqualTo(MateRecommendationBatchStatus.COMPLETED);
        assertThat(recommendationRepository.findAllByBatch_IdOrderByRecommendationRankAsc(batch.getId()))
                .hasSize(2);

        List<Long> firstRecommendationIds = first.recommendations().stream()
                .map(MateRecommendationCardQueryResult::recommendationId)
                .toList();
        MateRecommendationQueryResult second = queryService.retrieve(applicant.getId());

        assertThat(batchRepository.count()).isOne();
        assertThat(recommendationRepository.count()).isEqualTo(2);
        assertThat(second.recommendations())
                .extracting(MateRecommendationCardQueryResult::recommendationId)
                .containsExactlyElementsOf(firstRecommendationIds);
    }

    @Test
    @DisplayName("홈 카드는 현재 게시글·호스트 정보와 저장 점수·이유를 반환하며 숨김 후 대체하지 않는다")
    void mapsCurrentCardsAndPreservesSnapshotsWithoutReplacement() {
        User applicant = applicant("applicant");
        Destination jeju = destination("KR", "대한민국", "제주");
        User firstHost = host("first", "첫호스트", 5, 5, 5, 5);
        Post firstPost = post(firstHost, jeju, "첫 번째 여행", TODAY.plusDays(5), TODAY.plusDays(7));
        Post secondPost = post(host("second", "둘째호스트", 7, 7, 7, 7), jeju,
                "둘째 여행", TODAY.plusDays(5), TODAY.plusDays(7));
        MateRecommendationResponse first = homeService.retrieve(applicant.getId());
        assertThat(first.availabilityStatus()).isEqualTo(MateRecommendationResponse.AvailabilityStatus.AVAILABLE);
        assertThat(first.remainingFreeCount()).isZero();
        assertThat(first.recommendations()).extracting(MateRecommendationResponse.Item::postId)
                .containsExactly(firstPost.getId(), secondPost.getId());
        MateRecommendationResponse.Item original = first.recommendations().get(0);
        assertThat(original.location()).isEqualTo("대한민국 제주");
        assertThat(original.thumbnailUrl()).isEqualTo(firstPost.getPhotoUrl());
        assertThat(original.tags()).containsExactly("힐링", "맛집");
        assertThat(original.host().age()).isEqualTo(31);
        assertThat(original.host().profileImageInfo().url()).isNotBlank();
        assertThat(original.matchReasons()).extracting(MateRecommendationResponse.Reason::code)
                .contains("RHYTHM_MATCH");

        firstPost.updatePost(jeju, "수정된 여행", "수정된 여행방 본문으로 현재 카드 내용 반영을 검증합니다.",
                TODAY.plusDays(6), TODAY.plusDays(8), 5, TODAY.plusDays(4), Gender.U,
                true, null, null, "https://example.com/updated.jpg", "[\"산책\"]", CompanionType.FULL, TODAY);
        postRepository.saveAndFlush(firstPost);
        Profile profile = profileRepository.getByUserIdOrThrow(firstHost.getId());
        profile.updateNickname("새닉네임");
        profileRepository.saveAndFlush(profile);
        TravelTendency tendency = tendencyRepository.findByUserId(firstHost.getId()).orElseThrow();
        tendencyRepository.delete(tendency);
        tendency(firstHost, 9, 9, 9, 9);

        MateRecommendationResponse.Item updated = homeService.retrieve(applicant.getId()).recommendations().get(0);
        assertThat(updated.title()).isEqualTo("수정된 여행");
        assertThat(updated.thumbnailUrl()).isEqualTo("https://example.com/updated.jpg");
        assertThat(updated.description()).isEqualTo("수정된 여행방 본문으로 현재 카드 내용 반영을 검증합니다.");
        assertThat(updated.startDate()).isEqualTo(TODAY.plusDays(6));
        assertThat(updated.endDate()).isEqualTo(TODAY.plusDays(8));
        assertThat(updated.maxMemberCount()).isEqualTo(5);
        assertThat(updated.tags()).containsExactly("산책");
        assertThat(updated.host().nickname()).isEqualTo("새닉네임");
        assertThat(updated.matchPercentage()).isEqualTo(original.matchPercentage());
        assertThat(updated.matchReasons()).isEqualTo(original.matchReasons());
        assertThat(updated.cautionPoints()).isEqualTo(original.cautionPoints());

        firstPost.updatePost(jeju, null, null, null, null, 5, null, Gender.U,
                true, null, null, null, null, CompanionType.FULL, TODAY);
        postRepository.saveAndFlush(firstPost);
        MateRecommendationResponse withoutPhoto = homeService.retrieve(applicant.getId());
        assertThat(withoutPhoto.recommendations()).hasSize(2);
        assertThat(withoutPhoto.recommendations().get(0).thumbnailUrl()).isNull();

        post(host("replacement", "추가호스트", 5, 5, 5, 5), jeju,
                "추가 후보", TODAY.plusDays(5), TODAY.plusDays(7));
        firstPost.changeStatus(PostStatus.CLOSED);
        postRepository.saveAndFlush(firstPost);
        assertThat(homeService.retrieve(applicant.getId()).recommendations())
                .extracting(MateRecommendationResponse.Item::recommendationId)
                .containsExactly(first.recommendations().get(1).recommendationId());
        secondPost.changeStatus(PostStatus.CLOSED);
        postRepository.saveAndFlush(secondPost);
        MateRecommendationResponse empty = homeService.retrieve(applicant.getId());
        assertThat(empty.availabilityStatus()).isEqualTo(MateRecommendationResponse.AvailabilityStatus.AVAILABLE);
        assertThat(empty.recommendations()).isEmpty();
        assertThat(recommendationRepository.count()).isEqualTo(2);
        assertThat(batchRepository.count()).isOne();
    }

    @Test
    @DisplayName("사용자별 당일 묶음을 분리하고 KST 날짜가 바뀌면 새 추천 ID를 반환한다")
    void isolatesUsersAndCreatesNextDayBatch() {
        User first = applicant("applicant1");
        User second = applicant("applicant2");
        Destination jeju = destination("KR", "대한민국", "제주");
        post(host("host", "호스트", 5, 5, 5, 5), jeju, "제주 함께 여행", TODAY.plusDays(5), TODAY.plusDays(7));
        Long firstId = homeService.retrieve(first.getId()).recommendations().get(0).recommendationId();
        Long secondId = homeService.retrieve(second.getId()).recommendations().get(0).recommendationId();
        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(homeService.retrieve(first.getId()).recommendations().get(0).recommendationId()).isEqualTo(firstId);
        when(timeProvider.today()).thenReturn(TODAY.plusDays(1));
        Long nextId = homeService.retrieve(first.getId()).recommendations().get(0).recommendationId();
        assertThat(nextId).isNotIn(firstId, secondId);
        assertThat(batchRepository.findByUser_IdAndRecommendationDate(first.getId(), TODAY.plusDays(1))).isPresent();
        assertThat(batchRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("생성 중에는 빈 목록을 반환하고 실패한 묶음은 다음 조회에서 재시도한다")
    void generatingAndFailedBatch() {
        User applicant = applicant("applicant");
        MateRecommendationBatch batch = batchRepository.saveAndFlush(MateRecommendationBatch.create(applicant, TODAY));
        MateRecommendationResponse generating = homeService.retrieve(applicant.getId());
        assertThat(generating.availabilityStatus()).isEqualTo(MateRecommendationResponse.AvailabilityStatus.GENERATING);
        assertThat(generating.recommendations()).isEmpty();
        batch.fail("test failure");
        batchRepository.saveAndFlush(batch);
        MateRecommendationResponse empty = homeService.retrieve(applicant.getId());
        assertThat(empty.availabilityStatus()).isEqualTo(MateRecommendationResponse.AvailabilityStatus.AVAILABLE);
        assertThat(empty.recommendations()).isEmpty();
        assertThat(batchRepository.findById(batch.getId()).orElseThrow().getStatus()).isEqualTo(MateRecommendationBatchStatus.EMPTY);
        homeService.retrieve(applicant.getId());
        assertThat(batchRepository.count()).isOne();
    }

    @Test
    @DisplayName("설문 미완료와 설문 완료 후 정합성 오류는 추천 묶음을 생성하지 않는다")
    void validatesEligibilityBeforeCreatingBatch() {
        User applicant = user("applicant");
        assertThat(homeService.retrieve(applicant.getId()).availabilityStatus())
                .isEqualTo(MateRecommendationResponse.AvailabilityStatus.SURVEY_REQUIRED);
        onboardingRepository.saveAndFlush(new UserOnboarding(applicant));
        assertThat(homeService.retrieve(applicant.getId()).availabilityStatus())
                .isEqualTo(MateRecommendationResponse.AvailabilityStatus.SURVEY_REQUIRED);
        UserOnboarding onboarding = onboardingRepository.getByUserIdOrThrow(applicant.getId());
        onboarding.completeSurvey();
        onboardingRepository.saveAndFlush(onboarding);
        assertThatThrownBy(() -> homeService.retrieve(applicant.getId())).isInstanceOf(ProfileNotFoundException.class);
        profile(applicant, "신청자", Gender.F, LocalDate.of(2000, 1, 1));
        assertThatThrownBy(() -> homeService.retrieve(applicant.getId())).isInstanceOf(RecommendationTendencyMissingException.class);
        assertThat(batchRepository.count()).isZero();
    }

    private User applicant(String key) {
        User applicant = user(key);
        profile(applicant, key, Gender.F, LocalDate.of(2000, 1, 1));
        completeSurvey(applicant);
        tendency(applicant, 5, 5, 5, 5);
        return applicant;
    }

    private User host(
            String key,
            String nickname,
            double rhythm,
            double energy,
            double consumption,
            double decision
    ) {
        User host = user(key);
        profile(host, nickname, Gender.M, LocalDate.of(1995, 1, 1));
        tendency(host, rhythm, energy, consumption, decision);
        return host;
    }

    private User user(String key) {
        return userRepository.save(User.builder()
                .email(key + "@example.com")
                .name(key)
                .oauthId("oauth-" + key)
                .oauthType(OauthType.KAKAO)
                .build());
    }

    private void profile(User user, String nickname, Gender gender, LocalDate birthday) {
        Profile profile = new Profile(user);
        profile.setupInitialProfile(gender, null, birthday);
        profile.updateNickname(nickname);
        profileRepository.save(profile);
    }

    private void completeSurvey(User user) {
        UserOnboarding onboarding = new UserOnboarding(user);
        onboarding.completeSurvey();
        onboardingRepository.save(onboarding);
    }

    private void tendency(
            User user,
            double rhythm,
            double energy,
            double consumption,
            double decision
    ) {
        tendencyRepository.save(TravelTendency.create(
                user,
                BigDecimal.valueOf(rhythm),
                BigDecimal.valueOf(energy),
                BigDecimal.valueOf(consumption),
                BigDecimal.valueOf(decision),
                AvatarType.TTUR_POGUN
        ));
    }

    private Destination destination(String countryCode, String countryName, String city) {
        return destinationRepository.save(Destination.builder()
                .countryCode(countryCode)
                .countryName(countryName)
                .city(city)
                .image("https://example.com/" + city + ".jpg")
                .build());
    }

    private Post post(
            User host,
            Destination destination,
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return postRepository.save(Post.createPost(
                host,
                destination,
                title,
                "실제 추천 데이터 흐름을 검증하기 위한 충분한 길이의 여행방 본문입니다.",
                startDate,
                endDate,
                4,
                startDate.minusDays(1),
                Gender.U,
                true,
                null,
                null,
                "https://example.com/" + title + ".jpg",
                "[\"힐링\",\"맛집\"]",
                CompanionType.FULL
        ));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        JsonConverter jsonConverter() {
            return new JsonConverter(new ObjectMapper());
        }

        @Bean
        RecommendationAvailableDateMatcher recommendationAvailableDateMatcher() {
            return new RecommendationAvailableDateMatcher();
        }

        @Bean
        RecommendationScoreCalculator recommendationScoreCalculator() {
            return new RecommendationScoreCalculator();
        }

        @Bean
        RecommendationReasonJsonConverter recommendationReasonJsonConverter() {
            return new RecommendationReasonJsonConverter(new ObjectMapper());
        }
    }
}
