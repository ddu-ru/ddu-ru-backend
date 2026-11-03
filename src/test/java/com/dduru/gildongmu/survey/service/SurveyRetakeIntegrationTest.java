package com.dduru.gildongmu.survey.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.onboarding.service.OnboardingService;
import com.dduru.gildongmu.profile.service.ProfileManagementService;
import com.dduru.gildongmu.superhost.service.SuperHostService;
import com.dduru.gildongmu.survey.converter.SurveyConverter;
import com.dduru.gildongmu.survey.dto.request.SurveyRequest;
import com.dduru.gildongmu.survey.dto.response.AvatarProfileResponse;
import com.dduru.gildongmu.survey.exception.SurveyRetakeLockedException;
import com.dduru.gildongmu.survey.repository.SurveyRepository;
import com.dduru.gildongmu.survey.repository.TravelTendencyRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({QueryDslConfig.class, SurveyService.class, SurveyConverter.class,
        TravelTendencyCalculator.class, AvatarMatcher.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SurveyRetakeIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final SurveyRequest REQUEST = new SurveyRequest(
            1, 1, 1, List.of(1, 2, 3), 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);

    @Autowired private SurveyService service;
    @Autowired private SurveyConverter converter;
    @Autowired private SurveyRepository surveys;
    @Autowired private TravelTendencyRepository tendencies;
    @Autowired private UserRepository users;
    @MockitoBean private AvatarProfileService avatarProfiles;
    @MockitoBean private ProfileManagementService profiles;
    @MockitoBean private OnboardingService onboarding;
    @MockitoBean private SuperHostService superHost;
    @MockitoBean private TimeProvider timeProvider;
    private Long userId;
    private Long surveyId;

    @BeforeEach
    void setUp() {
        String key = UUID.randomUUID().toString();
        User user = users.save(User.builder().email(key + "@example.com").name("재테스트")
                .oauthId(key).oauthType(OauthType.KAKAO).build());
        userId = user.getId();
        surveyId = surveys.save(converter.toEntity(user, converter.parseRequest(REQUEST))).getId();
        when(timeProvider.today()).thenReturn(TODAY);
        when(avatarProfiles.getProfile(any())).thenReturn(new AvatarProfileResponse(
                "아바타", "설명", List.of(), "성격", "강점", "팁", "https://example.com/avatar.png"));
    }

    @AfterEach
    void tearDown() {
        tendencies.findByUserId(userId).ifPresent(tendencies::delete);
        surveys.deleteById(surveyId);
        users.deleteById(userId);
    }

    @Test
    void identicalAnswersConsumeFirstRetestAndEveryRetestRestartsCooldown() {
        service.update(userId, REQUEST);
        assertThat(surveys.getByUserIdOrThrow(userId).getLastRetestedAt()).isEqualTo(TODAY);
        assertThat(service.getMySurveyResult(userId).canRetake()).isFalse();
        assertThatThrownBy(() -> service.update(userId, REQUEST)).isInstanceOf(SurveyRetakeLockedException.class);

        when(timeProvider.today()).thenReturn(TODAY.plusDays(29));
        assertThat(service.getMySurveyResult(userId).remainingRetakeDays()).isEqualTo(1);
        assertThatThrownBy(() -> service.update(userId, REQUEST)).isInstanceOf(SurveyRetakeLockedException.class);

        when(timeProvider.today()).thenReturn(TODAY.plusDays(30));
        assertThat(service.getMySurveyResult(userId).canRetake()).isTrue();
        var response = service.update(userId, REQUEST);
        assertThat(response.nextRetakeAvailableDate()).isEqualTo(TODAY.plusDays(60));
        assertThat(surveys.getByUserIdOrThrow(userId).getLastRetestedAt()).isEqualTo(TODAY.plusDays(30));
        assertThat(service.getMySurveyResult(userId).canRetake()).isFalse();
    }

    @Test
    void failedRetestDoesNotConsumeOpportunity() {
        when(avatarProfiles.getProfile(any())).thenThrow(new IllegalStateException("profile failure"));
        assertThatThrownBy(() -> service.update(userId, REQUEST)).isInstanceOf(IllegalStateException.class);
        assertThat(surveys.getByUserIdOrThrow(userId).getLastRetestedAt()).isNull();
        assertThat(tendencies.findByUserId(userId)).isEmpty();
    }

    @Test
    void concurrentFirstRetestsAllowOnlyOneSubmission() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var futures = List.of(1, 2).stream().map(ignored -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    service.update(userId, REQUEST);
                    return true;
                } catch (SurveyRetakeLockedException exception) {
                    return false;
                }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(surveys.getByUserIdOrThrow(userId).getLastRetestedAt()).isEqualTo(TODAY);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
