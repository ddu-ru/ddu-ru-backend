package com.dduru.gildongmu.notification.listener;

import com.dduru.gildongmu.chat.event.PostUpdatedEvent;
import com.dduru.gildongmu.fcm.service.FcmPushService;
import com.dduru.gildongmu.journey.event.JourneyNoticeCreatedEvent;
import com.dduru.gildongmu.journey.event.ScheduleCanceledEvent;
import com.dduru.gildongmu.journey.event.ScheduleCreatedEvent;
import com.dduru.gildongmu.journey.event.ScheduleUpdatedEvent;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.like.repository.PostLikeRepository;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.service.NotificationPersistService;
import com.dduru.gildongmu.participation.event.MatchAppliedEvent;
import com.dduru.gildongmu.participation.event.MatchApprovedEvent;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.dduru.gildongmu.report.event.ReportReviewedEvent;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationPersistService notificationPersistService;

    @Mock
    private JourneyMemberRepository journeyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmPushService fcmPushService;

    @Mock
    private PostLikeRepository postLikeRepository;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationPersistService, journeyMemberRepository, userRepository, fcmPushService, postLikeRepository);
    }

    @Test
    void reportResultOnlyNotifiesReporter() {
        User reporter = User.builder().name("reporter").build();
        when(userRepository.getReferenceById(1L)).thenReturn(reporter);
        when(userRepository.findEnabledUserIds(List.of(1L))).thenReturn(List.of(1L));
        listener.handleReportReviewed(new ReportReviewedEvent(5L, 1L, ReportStatus.RESOLVED));
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationPersistService).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        Notification notification = captor.getValue().get(0);
        assertThat(notification.getRecipient()).isSameAs(reporter);
        assertThat(notification.getType()).isEqualTo(NotificationType.REPORT_REVIEWED);
        assertThat(notification.getResourceType()).isEqualTo(ResourceType.REPORT);
        assertThat(notification.getResourceId()).isEqualTo(5L);
        verify(fcmPushService).sendToUsers(List.of(1L), "신고 처리 결과", notification.getBody(), null,
                FcmPushService.dataPayload(ResourceType.REPORT, 5L));
    }

    // ── MATCH ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MATCH_APPLIED 이벤트")
    class HandleMatchApplied {

        @Test
        @DisplayName("이벤트를 수신하면 모집글 작성자에게 닉네임이 포함된 알림 1건을 저장한다")
        void savesNotificationForPostOwner() {
            Long participationId = 1L;
            Long actorUserId = 10L;
            Long recipientUserId = 20L;
            String postTitle = "일본 여행 모집";
            String actorNickname = "여행자닉네임";
            User recipient = createUser(recipientUserId);

            when(userRepository.getReferenceById(recipientUserId)).thenReturn(recipient);

            listener.handleMatchApplied(new MatchAppliedEvent(participationId, actorUserId, recipientUserId, postTitle, actorNickname));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationPersistService).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(NotificationType.MATCH_APPLIED);
            assertThat(saved.getBody()).isEqualTo("여행자닉네임 님이 매칭을 신청했습니다.");
            assertThat(saved.getResourceType()).isEqualTo(ResourceType.MATCH);
            assertThat(saved.getResourceId()).isEqualTo(participationId);
            assertThat(saved.isRead()).isFalse();
        }

        @Test
        @DisplayName("알림 저장 중 예외가 발생해도 예외가 전파되지 않는다")
        void exceptionIsSwallowed() {
            when(userRepository.getReferenceById(any())).thenThrow(new RuntimeException("DB 오류"));

            assertThatCode(() ->
                    listener.handleMatchApplied(new MatchAppliedEvent(1L, 10L, 20L, "test", "닉네임"))
            ).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MATCH_APPROVED 이벤트")
    class HandleMatchApproved {

        @Test
        @DisplayName("이벤트를 수신하면 신청자에게 승인자 닉네임이 포함된 JOURNEY 타입 알림을 저장한다")
        void savesNotificationForApplicantWithJourneyResource() {
            Long participationId = 1L;
            Long applicantUserId = 10L;
            Long journeyId = 30L;
            String journeyTitle = "도쿄 여정";
            String approverNickname = "호스트닉네임";
            User applicant = createUser(applicantUserId);

            when(userRepository.getReferenceById(applicantUserId)).thenReturn(applicant);

            listener.handleMatchApproved(new MatchApprovedEvent(participationId, applicantUserId, journeyId, journeyTitle, approverNickname));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationPersistService).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(NotificationType.MATCH_APPROVED);
            assertThat(saved.getBody()).isEqualTo("호스트닉네임 님과 매칭이 성사되었습니다.");
            assertThat(saved.getResourceType()).isEqualTo(ResourceType.JOURNEY);
            assertThat(saved.getResourceId()).isEqualTo(journeyId);
        }

        @Test
        @DisplayName("알림 저장 중 예외가 발생해도 예외가 전파되지 않는다")
        void exceptionIsSwallowed() {
            when(userRepository.getReferenceById(any())).thenThrow(new RuntimeException("DB 오류"));

            assertThatCode(() ->
                    listener.handleMatchApproved(new MatchApprovedEvent(1L, 10L, 30L, "여정", "닉네임"))
            ).doesNotThrowAnyException();
        }
    }

    // ── JOURNEY_NOTICE ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JOURNEY_NOTICE 이벤트")
    class HandleJourneyNoticeCreated {

        @Test
        @DisplayName("행위자 제외 후 수신자가 없으면 저장하지 않는다")
        void skipsWhenNoRecipients() {
            Long journeyId = 1L;
            Long actorUserId = 10L;

            when(journeyMemberRepository.findMemberIdsExcludingActor(journeyId, actorUserId))
                    .thenReturn(List.of());

            listener.handleJourneyNoticeCreated(
                    new JourneyNoticeCreatedEvent(5L, journeyId, "시부야 여정", actorUserId)
            );

            verify(notificationPersistService, never()).saveAll(any());
        }

        @Test
        @DisplayName("ACTIVE 멤버 전원(행위자 제외)에게 알림을 저장한다")
        void savesNotificationsForAllActiveMembers() {
            Long journeyPostId = 5L;
            Long journeyId = 1L;
            Long actorUserId = 10L;
            String journeyTitle = "시부야 여정";
            List<Long> recipientIds = List.of(20L, 30L);

            when(journeyMemberRepository.findMemberIdsExcludingActor(journeyId, actorUserId))
                    .thenReturn(recipientIds);
            when(userRepository.getReferenceById(20L)).thenReturn(createUser(20L));
            when(userRepository.getReferenceById(30L)).thenReturn(createUser(30L));

            listener.handleJourneyNoticeCreated(
                    new JourneyNoticeCreatedEvent(journeyPostId, journeyId, journeyTitle, actorUserId)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPersistService).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(n -> {
                assertThat(n.getType()).isEqualTo(NotificationType.JOURNEY_NOTICE);
                assertThat(n.getBody()).isEqualTo("시부야 여정 에 공지가 등록되었습니다.");
                assertThat(n.getResourceType()).isEqualTo(ResourceType.JOURNEY_POST);
                assertThat(n.getResourceId()).isEqualTo(journeyPostId);
                assertThat(n.isRead()).isFalse();
            });
        }

        @Test
        @DisplayName("알림 저장 중 예외가 발생해도 예외가 전파되지 않는다")
        void exceptionIsSwallowed() {
            when(journeyMemberRepository.findMemberIdsExcludingActor(any(), any()))
                    .thenThrow(new RuntimeException("DB 오류"));

            assertThatCode(() ->
                    listener.handleJourneyNoticeCreated(
                            new JourneyNoticeCreatedEvent(5L, 1L, "여정", 10L)
                    )
            ).doesNotThrowAnyException();
        }
    }

    // ── SCHEDULE ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SCHEDULE_CREATED 이벤트")
    class HandleScheduleCreated {

        @Test
        @DisplayName("ACTIVE 멤버 전원(행위자 제외)에게 일정 추가 알림을 저장한다")
        void savesNotificationsForAllActiveMembers() {
            Long scheduleId = 7L;
            Long journeyId = 1L;
            Long actorUserId = 10L;
            String scheduleTitle = "시부야 스크램블 집합";

            when(journeyMemberRepository.findMemberIdsExcludingActor(journeyId, actorUserId))
                    .thenReturn(List.of(20L));
            when(userRepository.getReferenceById(20L)).thenReturn(createUser(20L));

            listener.handleScheduleCreated(
                    new ScheduleCreatedEvent(scheduleId, journeyId, scheduleTitle, actorUserId)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPersistService).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getType()).isEqualTo(NotificationType.SCHEDULE_CREATED);
            assertThat(saved.get(0).getBody()).isEqualTo("시부야 스크램블 집합 일정이 추가되었습니다.");
            assertThat(saved.get(0).getResourceType()).isEqualTo(ResourceType.SCHEDULE);
            assertThat(saved.get(0).getResourceId()).isEqualTo(scheduleId);
        }

        @Test
        @DisplayName("수신자가 없으면 저장하지 않는다")
        void skipsWhenNoRecipients() {
            when(journeyMemberRepository.findMemberIdsExcludingActor(any(), any()))
                    .thenReturn(List.of());

            listener.handleScheduleCreated(new ScheduleCreatedEvent(7L, 1L, "일정", 10L));

            verify(notificationPersistService, never()).saveAll(any());
        }

        @Test
        @DisplayName("알림 저장 중 예외가 발생해도 예외가 전파되지 않는다")
        void exceptionIsSwallowed() {
            when(journeyMemberRepository.findMemberIdsExcludingActor(any(), any()))
                    .thenThrow(new RuntimeException("DB 오류"));

            assertThatCode(() ->
                    listener.handleScheduleCreated(new ScheduleCreatedEvent(7L, 1L, "일정", 10L))
            ).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("SCHEDULE_UPDATED 이벤트")
    class HandleScheduleUpdated {

        @Test
        @DisplayName("ACTIVE 멤버 전원(행위자 제외)에게 일정 변경 알림을 저장한다")
        void savesNotificationsWithUpdatedBody() {
            when(journeyMemberRepository.findMemberIdsExcludingActor(1L, 10L))
                    .thenReturn(List.of(20L));
            when(userRepository.getReferenceById(20L)).thenReturn(createUser(20L));

            listener.handleScheduleUpdated(new ScheduleUpdatedEvent(7L, 1L, "시부야 스크램블 집합", 10L));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPersistService).saveAll(captor.capture());

            assertThat(captor.getValue().get(0).getType()).isEqualTo(NotificationType.SCHEDULE_UPDATED);
            assertThat(captor.getValue().get(0).getBody()).isEqualTo("시부야 스크램블 집합 일정이 변경되었습니다.");
        }
    }

    @Nested
    @DisplayName("SCHEDULE_CANCELED 이벤트")
    class HandleScheduleCanceled {

        @Test
        @DisplayName("ACTIVE 멤버 전원(행위자 제외)에게 일정 취소 알림을 저장한다")
        void savesNotificationsWithCanceledBody() {
            when(journeyMemberRepository.findMemberIdsExcludingActor(1L, 10L))
                    .thenReturn(List.of(20L));
            when(userRepository.getReferenceById(20L)).thenReturn(createUser(20L));

            listener.handleScheduleCanceled(new ScheduleCanceledEvent(7L, 1L, "시부야 스크램블 집합", 10L));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPersistService).saveAll(captor.capture());

            assertThat(captor.getValue().get(0).getType()).isEqualTo(NotificationType.SCHEDULE_CANCELED);
            assertThat(captor.getValue().get(0).getBody()).isEqualTo("시부야 스크램블 집합 일정이 취소되었습니다.");
        }
    }

    // ── POST_UPDATED ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST_UPDATED 이벤트")
    class HandlePostUpdated {

        @Test
        @DisplayName("찜한 유저 전원에게 알림을 저장한다")
        void savesNotificationsForAllLikedUsers() {
            Long postId = 1L;
            List<Long> recipientIds = List.of(20L, 30L);

            when(postLikeRepository.findUserIdsByPostId(postId)).thenReturn(recipientIds);
            when(userRepository.getReferenceById(20L)).thenReturn(createUser(20L));
            when(userRepository.getReferenceById(30L)).thenReturn(createUser(30L));

            listener.handlePostUpdated(new PostUpdatedEvent(postId));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPersistService).saveAll(captor.capture());

            List<Notification> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(n -> {
                assertThat(n.getType()).isEqualTo(NotificationType.POST_UPDATED);
                assertThat(n.getBody()).isEqualTo("관심 있는 모집글에 변경이 있습니다.");
                assertThat(n.getResourceType()).isEqualTo(ResourceType.POST);
                assertThat(n.getResourceId()).isEqualTo(postId);
                assertThat(n.isRead()).isFalse();
            });
        }

        @Test
        @DisplayName("찜한 유저가 없으면 저장하지 않는다")
        void skipsWhenNoLikedUsers() {
            when(postLikeRepository.findUserIdsByPostId(any())).thenReturn(List.of());

            listener.handlePostUpdated(new PostUpdatedEvent(1L));

            verify(notificationPersistService, never()).saveAll(any());
        }

        @Test
        @DisplayName("알림 저장 중 예외가 발생해도 예외가 전파되지 않는다")
        void exceptionIsSwallowed() {
            when(postLikeRepository.findUserIdsByPostId(any())).thenThrow(new RuntimeException("DB 오류"));

            assertThatCode(() ->
                    listener.handlePostUpdated(new PostUpdatedEvent(1L))
            ).doesNotThrowAnyException();
        }
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────────────────────

    private User createUser(Long userId) {
        User user = User.builder()
                .email("user" + userId + "@example.com")
                .name("user" + userId)
                .oauthId("oauth-" + userId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
