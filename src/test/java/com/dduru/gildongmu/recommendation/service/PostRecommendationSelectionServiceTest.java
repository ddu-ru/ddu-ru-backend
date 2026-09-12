package com.dduru.gildongmu.recommendation.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.KoreaTime;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.onboarding.domain.UserOnboarding;
import com.dduru.gildongmu.onboarding.repository.UserOnboardingRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.domain.enums.ParticipationStatus;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.exception.ProfileNotFoundException;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.recommendation.domain.MateRecommendationPass;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationAvailableDate;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.dto.result.PostRecommendationResult;
import com.dduru.gildongmu.recommendation.dto.result.PostRecommendationResultStatus;
import com.dduru.gildongmu.recommendation.dto.result.ScoredPostRecommendation;
import com.dduru.gildongmu.recommendation.exception.RecommendationTendencyMissingException;
import com.dduru.gildongmu.recommendation.repository.ApplicantRecommendationQueryRepository;
import com.dduru.gildongmu.recommendation.repository.MateRecommendationPassRepository;
import com.dduru.gildongmu.recommendation.repository.RecommendablePostQueryRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationAvailableDateRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.recommendation.support.RecommendationAvailableDateMatcher;
import com.dduru.gildongmu.recommendation.support.RecommendationScoreCalculator;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.survey.domain.TravelTendency;
import com.dduru.gildongmu.survey.domain.enums.AvatarType;
import com.dduru.gildongmu.survey.repository.TravelTendencyRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QueryDslConfig.class, ApplicantRecommendationQueryRepository.class, RecommendablePostQueryRepository.class})
@DisplayName("PostRecommendationSelectionService 테스트")
class PostRecommendationSelectionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 4);
    private static final LocalDateTime NOW = TODAY.atTime(12, 0);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserOnboardingRepository userOnboardingRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TravelTendencyRepository travelTendencyRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRecommendationDestinationPreferenceRepository destinationPreferenceRepository;

    @Autowired
    private UserRecommendationAvailableDateRepository availableDateRepository;

    @Autowired
    private MateRecommendationPassRepository recommendationPassRepository;

    @Autowired
    private ApplicantRecommendationQueryRepository applicantRecommendationQueryRepository;

    @Autowired
    private RecommendablePostQueryRepository recommendablePostQueryRepository;

    private PostRecommendationSelectionService recommendationSelectionService;
    private int sequence;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = new TimeProvider(Clock.fixed(
                LocalDateTime.of(2026, 7, 4, 12, 0).atZone(KoreaTime.ZONE_ID).toInstant(),
                KoreaTime.ZONE_ID
        ));
        RecommendationApplicantContextResolver contextResolver = new RecommendationApplicantContextResolver(
                timeProvider,
                applicantRecommendationQueryRepository,
                destinationPreferenceRepository,
                availableDateRepository
        );
        recommendationSelectionService = new PostRecommendationSelectionService(
                contextResolver,
                recommendablePostQueryRepository,
                new RecommendationAvailableDateMatcher(),
                new RecommendationScoreCalculator()
        );
    }

    @Test
    @DisplayName("설문 미완료는 SURVEY_REQUIRED 상태를 반환한다")
    void unavailableWhenRequiredApplicantInputsMissing() {
        User surveyRequiredUser = applicant("survey-required", Gender.F, LocalDate.of(2000, 7, 4), false, true);

        PostRecommendationResult surveyRequired = recommendationSelectionService.selectRecommendations(surveyRequiredUser.getId());

        assertThat(surveyRequired.status()).isEqualTo(PostRecommendationResultStatus.SURVEY_REQUIRED);
    }

    @Test
    @DisplayName("설문 완료 사용자의 프로필이 없으면 데이터 정합성 오류로 예외가 발생한다")
    void throwsWhenCompletedSurveyUserHasNoProfile() {
        User noProfileUser = user("no-profile");
        completeSurvey(noProfileUser);
        saveTendency(noProfileUser, 5, 5, 5, 5);

        assertThatThrownBy(() -> recommendationSelectionService.selectRecommendations(noProfileUser.getId()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    @DisplayName("설문 완료 사용자의 성향 점수가 없으면 데이터 정합성 오류로 예외가 발생한다")
    void throwsWhenCompletedSurveyUserHasNoTendency() {
        User noTendencyUser = user("no-tendency");
        saveProfile(noTendencyUser, Gender.F, LocalDate.of(2000, 7, 4));
        completeSurvey(noTendencyUser);

        assertThatThrownBy(() -> recommendationSelectionService.selectRecommendations(noTendencyUser.getId()))
                .isInstanceOf(RecommendationTendencyMissingException.class);
    }

    @Test
    @DisplayName("추천 가능 사용자지만 후보가 없으면 NO_CANDIDATES 상태와 빈 목록을 반환한다")
    void returnsNoCandidatesStatusWhenNoRecommendablePosts() {
        User applicant = applicant("applicant-no-candidates", Gender.F, LocalDate.of(2000, 7, 4), true, true);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.status()).isEqualTo(PostRecommendationResultStatus.NO_CANDIDATES);
        assertThat(result.recommendations()).isEmpty();
    }

    @Test
    @DisplayName("선택 필터가 없으면 기본 제외 조건만 적용하고 점수, 시작일, postId 순으로 최대 3개를 정렬한다")
    void selectsRecommendationsWithoutOptionalFiltersAndSortsTopThree() {
        User applicant = applicant("applicant-sort", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");

        Post later = openPost(hostWithTendency("host-later", 5, 5, 5, 5), jeju, TODAY.plusDays(7), TODAY.plusDays(9), CompanionType.FULL);
        Post earlierOldId = openPost(hostWithTendency("host-earlier-old", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);
        Post earlierNewId = openPost(hostWithTendency("host-earlier-new", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);
        openPost(hostWithTendency("host-low-score", 0, 0, 0, 0), jeju, TODAY.plusDays(1), TODAY.plusDays(3), CompanionType.FULL);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.status()).isEqualTo(PostRecommendationResultStatus.READY);
        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(earlierNewId.getId(), earlierOldId.getId(), later.getId());
        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::matchPercentage)
                .containsExactly(100, 100, 100);
    }

    @Test
    @DisplayName("자기 게시글, 참여 신청, 패스, 신고, 호스트 성향 없음 게시글을 제외한다")
    void excludesAlreadyHandledOrInvalidPosts() {
        User applicant = applicant("applicant-exclude", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        Post valid = openPost(hostWithTendency("host-valid", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);
        openPost(applicant, jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);
        participationRepository.save(Participation.createParticipation(
                openPost(hostWithTendency("host-participation", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL),
                applicant,
                "참여 신청"
        ));
        recommendationPassRepository.save(MateRecommendationPass.of(
                applicant,
                openPost(hostWithTendency("host-pass", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL)
        ));
        reportRepository.save(Report.createReport(
                applicant,
                openPost(hostWithTendency("host-report", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL),
                ReportReason.OTHER,
                "신고"
        ));
        openPost(user("host-no-tendency"), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(valid.getId());
    }

    @Test
    @DisplayName("참여 신청 상태와 무관하게 참여 row가 있으면 후보에서 제외한다")
    void excludesAllParticipationStatuses() {
        User applicant = applicant("applicant-participation-status", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        Post valid = openPost(hostWithTendency("host-valid-status", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);

        for (ParticipationStatus status : ParticipationStatus.values()) {
            Participation participation = Participation.createParticipation(
                    openPost(hostWithTendency("host-" + status, 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL),
                    applicant,
                    "참여 신청"
            );
            applyStatus(participation, status);
            participationRepository.save(participation);
        }

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(valid.getId());
    }

    @Test
    @DisplayName("관심 여행지는 CITY exact match와 COUNTRY country_code match를 OR 조건으로 적용한다")
    void appliesDestinationPreferences() {
        User applicant = applicant("applicant-destination", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        Destination busan = destination("KR", "대한민국", "부산");
        Destination tokyo = destination("JP", "일본", "도쿄");
        Destination paris = destination("FR", "프랑스", "파리");
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.country(applicant, "KR", 1));
        destinationPreferenceRepository.save(UserRecommendationDestinationPreference.city(applicant, tokyo, 2));

        Post jejuPost = openPost(hostWithTendency("host-jeju", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL);
        Post busanPost = openPost(hostWithTendency("host-busan", 5, 5, 5, 5), busan, TODAY.plusDays(6), TODAY.plusDays(8), CompanionType.FULL);
        Post tokyoPost = openPost(hostWithTendency("host-tokyo", 5, 5, 5, 5), tokyo, TODAY.plusDays(7), TODAY.plusDays(9), CompanionType.FULL);
        openPost(hostWithTendency("host-paris", 5, 5, 5, 5), paris, TODAY.plusDays(1), TODAY.plusDays(3), CompanionType.FULL);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(jejuPost.getId(), busanPost.getId(), tokyoPost.getId());
    }

    @Test
    @DisplayName("가능 날짜는 동행 방식별 FULL, PARTIAL, MEAL 기준으로 적용한다")
    void appliesAvailableDatePreferences() {
        User applicant = applicant("applicant-date", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        availableDateRepository.save(UserRecommendationAvailableDate.of(applicant, date(2026, 7, 10), date(2026, 7, 12)));

        Post full = openPost(hostWithTendency("host-full", 5, 5, 5, 5), jeju, date(2026, 7, 10), date(2026, 7, 12), CompanionType.FULL);
        openPost(hostWithTendency("host-full-fail", 5, 5, 5, 5), jeju, date(2026, 7, 10), date(2026, 7, 13), CompanionType.FULL);
        Post partial = openPost(hostWithTendency("host-partial", 5, 5, 5, 5), jeju, date(2026, 7, 11), date(2026, 7, 13), CompanionType.PARTIAL);
        openPost(hostWithTendency("host-partial-fail", 5, 5, 5, 5), jeju, date(2026, 7, 12), date(2026, 7, 13), CompanionType.PARTIAL);
        Post meal = openPost(hostWithTendency("host-meal", 5, 5, 5, 5), jeju, date(2026, 7, 12), date(2026, 7, 14), CompanionType.MEAL);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(full.getId(), partial.getId(), meal.getId());
    }

    @Test
    @DisplayName("UNSPECIFIED 동행 방식은 날짜가 1일 이상 겹치면 후보가 된다")
    void appliesUnspecifiedCompanionTypeDatePreference() {
        User applicant = applicant("applicant-unspecified-date", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        availableDateRepository.save(UserRecommendationAvailableDate.of(applicant, date(2026, 7, 10), date(2026, 7, 12)));

        Post unspecified = openPost(hostWithTendency("host-unspecified", 5, 5, 5, 5), jeju, date(2026, 7, 12), date(2026, 7, 14), CompanionType.UNSPECIFIED);
        openPost(hostWithTendency("host-unspecified-fail", 5, 5, 5, 5), jeju, date(2026, 7, 13), date(2026, 7, 14), CompanionType.UNSPECIFIED);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(unspecified.getId());
    }

    @Test
    @DisplayName("게시글 선호 성별과 나이 범위를 만족하는 게시글만 후보가 된다")
    void appliesPreferredGenderAndAge() {
        User applicant = applicant("applicant-gender-age", Gender.F, LocalDate.of(2000, 7, 4), true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        Post genderAny = openPost(hostWithTendency("host-gender-any", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL, Gender.U, true, null, null);
        Post femaleAgeMatch = openPost(hostWithTendency("host-female-age-match", 5, 5, 5, 5), jeju, TODAY.plusDays(6), TODAY.plusDays(8), CompanionType.FULL, Gender.F, false, 20, 30);
        openPost(hostWithTendency("host-male", 5, 5, 5, 5), jeju, TODAY.plusDays(1), TODAY.plusDays(3), CompanionType.FULL, Gender.M, true, null, null);
        openPost(hostWithTendency("host-age-mismatch", 5, 5, 5, 5), jeju, TODAY.plusDays(2), TODAY.plusDays(4), CompanionType.FULL, Gender.F, false, 27, 30);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(genderAny.getId(), femaleAgeMatch.getId());
    }

    @Test
    @DisplayName("성별 또는 생년월일이 필요한 조건인데 신청자 프로필 값이 없으면 제외한다")
    void excludesWhenRequiredApplicantProfileValuesAreMissing() {
        User applicant = applicant("applicant-profile-missing", null, null, true, true);
        Destination jeju = destination("KR", "대한민국", "제주");
        Post genderAnyAgeAny = openPost(hostWithTendency("host-any-any", 5, 5, 5, 5), jeju, TODAY.plusDays(5), TODAY.plusDays(7), CompanionType.FULL, Gender.U, true, null, null);
        openPost(hostWithTendency("host-gender-required", 5, 5, 5, 5), jeju, TODAY.plusDays(1), TODAY.plusDays(3), CompanionType.FULL, Gender.F, true, null, null);
        openPost(hostWithTendency("host-age-required", 5, 5, 5, 5), jeju, TODAY.plusDays(2), TODAY.plusDays(4), CompanionType.FULL, Gender.U, false, 20, 30);

        PostRecommendationResult result = recommendationSelectionService.selectRecommendations(applicant.getId());

        assertThat(result.recommendations())
                .extracting(ScoredPostRecommendation::postId)
                .containsExactly(genderAnyAgeAny.getId());
    }

    private User applicant(String suffix, Gender gender, LocalDate birthday, boolean surveyCompleted, boolean withTendency) {
        User user = user(suffix);
        saveProfile(user, gender, birthday);
        if (surveyCompleted) {
            completeSurvey(user);
        } else {
            userOnboardingRepository.save(new UserOnboarding(user));
        }
        if (withTendency) {
            saveTendency(user, 5, 5, 5, 5);
        }
        return user;
    }

    private User hostWithTendency(String suffix, double rhythm, double energy, double consumption, double decision) {
        User user = user(suffix);
        saveTendency(user, rhythm, energy, consumption, decision);
        return user;
    }

    private User user(String suffix) {
        sequence++;
        return userRepository.save(User.builder()
                .email(suffix + sequence + "@example.com")
                .name("사용자" + sequence)
                .oauthId("oauth-" + suffix + "-" + sequence)
                .oauthType(OauthType.KAKAO)
                .build());
    }

    private Profile saveProfile(User user, Gender gender, LocalDate birthday) {
        Profile profile = new Profile(user);
        profile.setupInitialProfile(gender, null, birthday);
        return profileRepository.save(profile);
    }

    private void completeSurvey(User user) {
        UserOnboarding onboarding = new UserOnboarding(user);
        onboarding.completeSurvey();
        userOnboardingRepository.save(onboarding);
    }

    private TravelTendency saveTendency(User user, double rhythm, double energy, double consumption, double decision) {
        return travelTendencyRepository.save(TravelTendency.create(
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

    private Post openPost(User host, Destination destination, LocalDate startDate, LocalDate endDate, CompanionType companionType) {
        return openPost(host, destination, startDate, endDate, companionType, Gender.U, true, null, null);
    }

    private Post openPost(
            User host,
            Destination destination,
            LocalDate startDate,
            LocalDate endDate,
            CompanionType companionType,
            Gender preferredGender,
            boolean isAgeAny,
            Integer minAge,
            Integer maxAge
    ) {
        sequence++;
        return postRepository.save(Post.createPost(
                host,
                destination,
                "추천 테스트 제목 " + sequence,
                "추천 후보 조회 테스트를 위한 충분한 길이의 본문입니다.",
                startDate,
                endDate,
                4,
                endDate.minusDays(1),
                preferredGender,
                isAgeAny,
                minAge,
                maxAge,
                "https://example.com/post-" + sequence + ".jpg",
                "[\"테스트\"]",
                companionType
        ));
    }

    private void applyStatus(Participation participation, ParticipationStatus status) {
        if (status == ParticipationStatus.CONTACTING) {
            participation.contact(NOW);
        }
        if (status == ParticipationStatus.APPROVED) {
            participation.approve(NOW);
        }
        if (status == ParticipationStatus.REJECTED) {
            participation.reject(NOW);
        }
    }

    private LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }
}
