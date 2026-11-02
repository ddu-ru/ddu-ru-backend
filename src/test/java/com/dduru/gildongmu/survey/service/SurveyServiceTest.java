package com.dduru.gildongmu.survey.service;

import com.dduru.gildongmu.auth.exception.UserNotFoundException;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.onboarding.service.OnboardingService;
import com.dduru.gildongmu.profile.service.ProfileManagementService;
import com.dduru.gildongmu.survey.converter.SurveyConverter;
import com.dduru.gildongmu.survey.converter.ParsedSurveyData;
import com.dduru.gildongmu.survey.domain.AvatarProfile;
import com.dduru.gildongmu.survey.domain.Survey;
import com.dduru.gildongmu.survey.domain.TravelTendency;
import com.dduru.gildongmu.survey.domain.enums.*;
import com.dduru.gildongmu.survey.dto.response.AvatarProfileResponse;
import com.dduru.gildongmu.survey.dto.request.SurveyRequest;
import com.dduru.gildongmu.survey.dto.response.SurveyResponse;
import com.dduru.gildongmu.survey.dto.response.TendencyScoreResponse;
import com.dduru.gildongmu.survey.exception.SurveyAlreadySubmittedException;
import com.dduru.gildongmu.survey.exception.SurveyRetakeLockedException;
import com.dduru.gildongmu.survey.exception.SurveyResultNotFoundException;
import com.dduru.gildongmu.survey.repository.AvatarProfileRepository;
import com.dduru.gildongmu.survey.repository.SurveyRepository;
import com.dduru.gildongmu.survey.repository.TravelTendencyRepository;
import com.dduru.gildongmu.superhost.service.SuperHostService;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("설문조사 서비스 테스트")
class SurveyServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private TravelTendencyRepository travelTendencyRepository;
    @Mock
    private SurveyConverter surveyConverter;
    @Mock
    private TravelTendencyCalculator tendencyCalculator;
    @Mock
    private AvatarMatcher avatarMatcher;
    @Mock
    private AvatarProfileService avatarProfileService;
    @Mock
    private AvatarProfileRepository avatarProfileRepository;
    @Mock
    private ProfileManagementService profileManagementService;
    @Mock
    private OnboardingService onboardingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SuperHostService superHostService;
    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private SurveyService surveyService;

    private User testUser;
    private SurveyRequest testRequest;
    private Survey testSurvey;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트")
                .oauthId("12345")
                .oauthType(OauthType.KAKAO)
                .build();

        testRequest = new SurveyRequest(
                1, 1, 1,
                List.of(1, 2, 3),
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1
        );

        ReflectionTestUtils.setField(testUser, "id", 1L);

        testSurvey = Survey.createSurvey(
                testUser,
                RhythmQuestion1.PLANNED_ROUTE,
                RhythmQuestion2.PACK_EARLY,
                RhythmQuestion3.ROUTE_TIME_SET,
                ConsumptionQuestion1.ADJUST_BUDGET,
                ConsumptionQuestion2.VALUE_TRANSPORT,
                ConsumptionQuestion3.VALUE_CHOICE,
                EnergyQuestion1.RELAXED_DAY,
                EnergyQuestion2.BRUNCH_INSTEAD,
                EnergyQuestion3.DO_NOTHING_OK,
                DecisionQuestion1.DELEGATE_ROLE,
                DecisionQuestion2.FOLLOW_OTHERS,
                DecisionQuestion3.WAIT_AND_SEE,
                RecordStyleQuestion.EYES_FIRST,
                List.of(ActivityTag.SIGHTSEEING, ActivityTag.EXHIBITION, ActivityTag.NATURE)
        );
        ReflectionTestUtils.setField(testSurvey, "createdAt", TODAY.minusDays(30).atStartOfDay());
        ReflectionTestUtils.setField(testSurvey, "modifiedAt", TODAY.minusDays(30).atStartOfDay());

        lenient().when(timeProvider.today()).thenReturn(TODAY);
    }

    @Test
    @DisplayName("새로운_설문_제출_성공")
    void 새로운_설문_제출_성공() {
        ParsedSurveyData parsedData = new ParsedSurveyData(
                RhythmQuestion1.PLANNED_ROUTE,
                RhythmQuestion2.PACK_EARLY,
                RhythmQuestion3.ROUTE_TIME_SET,
                ConsumptionQuestion1.ADJUST_BUDGET,
                ConsumptionQuestion2.VALUE_TRANSPORT,
                ConsumptionQuestion3.VALUE_CHOICE,
                EnergyQuestion1.RELAXED_DAY,
                EnergyQuestion2.BRUNCH_INSTEAD,
                EnergyQuestion3.DO_NOTHING_OK,
                DecisionQuestion1.DELEGATE_ROLE,
                DecisionQuestion2.FOLLOW_OTHERS,
                DecisionQuestion3.WAIT_AND_SEE,
                RecordStyleQuestion.EYES_FIRST,
                List.of(ActivityTag.SIGHTSEEING, ActivityTag.EXHIBITION, ActivityTag.NATURE)
        );

        when(surveyRepository.existsByUserId(1L)).thenReturn(false);
        when(userRepository.getByIdOrThrow(1L)).thenReturn(testUser);
        when(surveyConverter.parseRequest(testRequest)).thenReturn(parsedData);
        when(surveyConverter.toEntity(testUser, parsedData)).thenReturn(testSurvey);
        when(surveyRepository.save(any(Survey.class))).thenReturn(testSurvey);

        TendencyScoreResponse scores =
                new TendencyScoreResponse(5.5, 4.5, 6.0, 7.0);
        when(tendencyCalculator.calculate(testSurvey)).thenReturn(scores);
        when(avatarMatcher.match(scores.rhythmScore(), scores.energyScore(), scores.consumptionScore(), scores.decisionScore()))
                .thenReturn(AvatarType.TTUR_DASOM);

        AvatarProfileResponse profile = new AvatarProfileResponse(
                "뚜르 스윗",
                "설명",
                List.of("태그1", "태그2", "태그3"),
                "성격",
                "강점",
                "팁",
                "https://example.com/avatar-sweet.png"
        );
        when(avatarProfileService.getProfile(AvatarType.TTUR_DASOM)).thenReturn(profile);

        AvatarProfile avatarProfile = AvatarProfile.builder()
                .avatarType(AvatarType.TTUR_DASOM)
                .displayName("뚜르 스윗")
                .speechBubbleText("설명")
                .descriptionLine1("성격")
                .descriptionLine2("강점")
                .descriptionLine3("팁")
                .imageUrl("https://example.com/avatar-sweet.png")
                .tags("[]")
                .build();
        ReflectionTestUtils.setField(avatarProfile, "id", 1L);
        when(avatarProfileRepository.findByAvatarType(AvatarType.TTUR_DASOM)).thenReturn(Optional.of(avatarProfile));

        when(travelTendencyRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(travelTendencyRepository.save(any(TravelTendency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurveyResponse response = surveyService.create(1L, testRequest);

        assertThat(response.tendencyScores().rhythmScore()).isEqualTo(5.5);
        assertThat(response.tendencyScores().energyScore()).isEqualTo(4.5);
        assertThat(response.tendencyScores().consumptionScore()).isEqualTo(6.0);
        assertThat(response.tendencyScores().decisionScore()).isEqualTo(7.0);
        assertThat(response.avatarType()).isEqualTo(AvatarType.TTUR_DASOM);
        assertThat(response.avatarProfile().characterName()).isEqualTo("뚜르 스윗");
        assertThat(response.avatarProfile().speechBubbleText()).isEqualTo("설명");
        assertThat(response.avatarProfile().tags()).containsExactly("태그1", "태그2", "태그3");
        assertThat(response.avatarProfile().descriptionLine1()).isEqualTo("성격");
        assertThat(response.avatarProfile().descriptionLine2()).isEqualTo("강점");
        assertThat(response.avatarProfile().descriptionLine3()).isEqualTo("팁");
        assertThat(response.avatarProfile().imageUrl()).isEqualTo("https://example.com/avatar-sweet.png");
        assertThat(response.recordStyleType()).isEqualTo(RecordStyleType.A);
        assertThat(response.avatarLabel()).isEqualTo("뚜르 스윗-A");
        assertThat(response.lastTestedAt()).isEqualTo(TODAY);
        assertThat(response.canRetake()).isTrue();
        assertThat(response.nextRetakeAvailableDate()).isEqualTo(TODAY);
        assertThat(response.remainingRetakeDays()).isZero();
        assertThat(testSurvey.getLastRetestedAt()).isNull();

        verify(surveyRepository).save(any(Survey.class));
        verify(travelTendencyRepository).save(any(TravelTendency.class));
        verify(superHostService).grantOnboardingRewardTicket(1L);
    }

    @Test
    @DisplayName("이미_제출된_설문_중복_제출_예외발생")
    void 이미_제출된_설문_중복_제출_예외발생() {
        when(surveyRepository.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> surveyService.create(1L, testRequest))
                .isInstanceOf(SurveyAlreadySubmittedException.class);

        verify(surveyRepository, never()).save(any());
        verify(userRepository, never()).getByIdOrThrow(any());
    }

    @Test
    @DisplayName("존재하지_않는_사용자_예외발생")
    void 존재하지_않는_사용자_예외발생() {
        when(surveyRepository.existsByUserId(999L)).thenReturn(false);
        when(userRepository.getByIdOrThrow(999L)).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> surveyService.create(999L, testRequest))
                .isInstanceOf(UserNotFoundException.class);

        verify(surveyRepository, never()).save(any());
        verify(travelTendencyRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 29, 30, 31})
    @DisplayName("설문 결과 조회 시 최초 테스트와 재테스트 경과일에 맞는 가능 여부 반환")
    void 설문_결과_조회_성공(int daysSinceRetest) {
        if (daysSinceRetest >= 0) {
            testSurvey.recordRetest(TODAY.minusDays(daysSinceRetest));
        } else {
            ReflectionTestUtils.setField(testSurvey, "modifiedAt", TODAY.minusDays(1).atStartOfDay());
        }
        TravelTendency travelTendency = TravelTendency.create(
                testUser,
                BigDecimal.valueOf(5.5),
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(6.0),
                BigDecimal.valueOf(7.0),
                AvatarType.TTUR_DASOM
        );

        when(travelTendencyRepository.findByUserId(1L)).thenReturn(Optional.of(travelTendency));
        when(surveyRepository.getByUserIdOrThrow(1L)).thenReturn(testSurvey);

        AvatarProfileResponse profile = new AvatarProfileResponse(
                "뚜르 스윗",
                "설명",
                List.of("태그1", "태그2", "태그3"),
                "성격",
                "강점",
                "팁",
                "https://example.com/avatar-sweet.png"
        );
        when(avatarProfileService.getProfile(AvatarType.TTUR_DASOM)).thenReturn(profile);

        SurveyResponse response = surveyService.getMySurveyResult(1L);

        assertThat(response.tendencyScores().rhythmScore()).isEqualTo(5.5);
        assertThat(response.tendencyScores().energyScore()).isEqualTo(4.5);
        assertThat(response.tendencyScores().consumptionScore()).isEqualTo(6.0);
        assertThat(response.tendencyScores().decisionScore()).isEqualTo(7.0);
        assertThat(response.avatarType()).isEqualTo(AvatarType.TTUR_DASOM);
        assertThat(response.avatarCode()).isEqualTo(4);
        assertThat(response.avatarProfile().imageUrl()).isEqualTo("https://example.com/avatar-sweet.png");
        assertThat(response.recordStyleType()).isEqualTo(RecordStyleType.A);
        assertThat(response.avatarLabel()).isEqualTo("뚜르 스윗-A");
        assertThat(response.lastTestedAt()).isEqualTo(
                daysSinceRetest < 0 ? TODAY.minusDays(1) : TODAY.minusDays(daysSinceRetest));
        assertThat(response.canRetake()).isEqualTo(daysSinceRetest < 0 || daysSinceRetest >= 30);
        assertThat(response.nextRetakeAvailableDate()).isEqualTo(
                daysSinceRetest < 0 ? TODAY : TODAY.minusDays(daysSinceRetest).plusDays(30));
        assertThat(response.remainingRetakeDays()).isEqualTo(
                daysSinceRetest < 0 ? 0 : Math.max(0, 30 - daysSinceRetest));
    }

    @Test
    @DisplayName("설문_결과_없을때_예외발생")
    void 설문_결과_없을때_예외발생() {
        when(travelTendencyRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.getMySurveyResult(1L))
                .isInstanceOf(SurveyResultNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 30, 31})
    @DisplayName("첫 재테스트는 즉시 허용하고 이후에는 30일 경과 시 수정 및 제한 갱신")
    void 마이페이지_성향테스트_수정_성공_온보딩_보상_미호출(int daysSinceRetest) {
        ReflectionTestUtils.setField(testSurvey, "createdAt", TODAY.atStartOfDay());
        ReflectionTestUtils.setField(testSurvey, "modifiedAt", TODAY.atStartOfDay());
        if (daysSinceRetest >= 0) {
            testSurvey.recordRetest(TODAY.minusDays(daysSinceRetest));
        }
        ParsedSurveyData parsedData = new ParsedSurveyData(
                RhythmQuestion1.PLANNED_ROUTE,
                RhythmQuestion2.PACK_EARLY,
                RhythmQuestion3.ROUTE_TIME_SET,
                ConsumptionQuestion1.ADJUST_BUDGET,
                ConsumptionQuestion2.VALUE_TRANSPORT,
                ConsumptionQuestion3.VALUE_CHOICE,
                EnergyQuestion1.RELAXED_DAY,
                EnergyQuestion2.BRUNCH_INSTEAD,
                EnergyQuestion3.DO_NOTHING_OK,
                DecisionQuestion1.DELEGATE_ROLE,
                DecisionQuestion2.FOLLOW_OTHERS,
                DecisionQuestion3.WAIT_AND_SEE,
                RecordStyleQuestion.EYES_FIRST,
                List.of(ActivityTag.SIGHTSEEING)
        );

        when(userRepository.getByIdOrThrow(1L)).thenReturn(testUser);
        when(surveyRepository.getByUserIdForUpdateOrThrow(1L)).thenReturn(testSurvey);
        when(surveyConverter.parseRequest(testRequest)).thenReturn(parsedData);

        TendencyScoreResponse scores = new TendencyScoreResponse(5.5, 4.5, 6.0, 7.0);
        when(tendencyCalculator.calculate(testSurvey)).thenReturn(scores);
        when(avatarMatcher.match(scores.rhythmScore(), scores.energyScore(), scores.consumptionScore(), scores.decisionScore()))
                .thenReturn(AvatarType.TTUR_DASOM);

        AvatarProfileResponse profile = new AvatarProfileResponse(
                "뚜르 스윗", "말풍선", List.of("태그1", "태그2", "태그3"), "성격", "강점", "팁",
                "https://example.com/avatar-sweet.png"
        );
        when(avatarProfileService.getProfile(AvatarType.TTUR_DASOM)).thenReturn(profile);

        AvatarProfile avatarProfile = AvatarProfile.builder()
                .avatarType(AvatarType.TTUR_DASOM)
                .displayName("뚜르 스윗")
                .speechBubbleText("말풍선")
                .descriptionLine1("성격")
                .descriptionLine2("강점")
                .descriptionLine3("팁")
                .imageUrl("https://example.com/avatar-sweet.png")
                .tags("[]")
                .build();
        ReflectionTestUtils.setField(avatarProfile, "id", 1L);
        when(avatarProfileRepository.findByAvatarType(AvatarType.TTUR_DASOM)).thenReturn(Optional.of(avatarProfile));

        when(travelTendencyRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(travelTendencyRepository.save(any(TravelTendency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SurveyResponse response = surveyService.update(1L, testRequest);

        assertThat(response.avatarType()).isEqualTo(AvatarType.TTUR_DASOM);
        assertThat(response.lastTestedAt()).isEqualTo(TODAY);
        assertThat(response.canRetake()).isFalse();
        assertThat(response.nextRetakeAvailableDate()).isEqualTo(TODAY.plusDays(30));
        assertThat(response.remainingRetakeDays()).isEqualTo(30);
        assertThat(testSurvey.getLastRetestedAt()).isEqualTo(TODAY);
        assertThatThrownBy(() -> surveyService.update(1L, testRequest))
                .isInstanceOf(SurveyRetakeLockedException.class);
        verify(surveyConverter, times(1)).parseRequest(testRequest);
        verify(onboardingService, never()).completeSurvey(any());
        verify(superHostService, never()).grantOnboardingRewardTicket(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 29})
    @DisplayName("마이페이지_성향테스트_수정_30일_쿨다운_전이면_예외발생")
    void 마이페이지_성향테스트_수정_30일_쿨다운_전이면_예외발생(int daysSinceRetest) {
        testSurvey.recordRetest(TODAY.minusDays(daysSinceRetest));
        when(userRepository.getByIdOrThrow(1L)).thenReturn(testUser);
        when(surveyRepository.getByUserIdForUpdateOrThrow(1L)).thenReturn(testSurvey);

        assertThatThrownBy(() -> surveyService.update(1L, testRequest))
                .isInstanceOf(SurveyRetakeLockedException.class);

        verify(surveyConverter, never()).parseRequest(any());
        verify(tendencyCalculator, never()).calculate(any());
        verify(travelTendencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("마이페이지_성향테스트_수정_설문없으면_예외발생")
    void 마이페이지_성향테스트_수정_설문없으면_예외발생() {
        when(userRepository.getByIdOrThrow(1L)).thenReturn(testUser);
        when(surveyRepository.getByUserIdForUpdateOrThrow(1L)).thenThrow(SurveyResultNotFoundException.class);

        assertThatThrownBy(() -> surveyService.update(1L, testRequest))
                .isInstanceOf(SurveyResultNotFoundException.class);

        verify(tendencyCalculator, never()).calculate(any());
        verify(onboardingService, never()).completeSurvey(any());
        verify(superHostService, never()).grantOnboardingRewardTicket(any());
    }
}
