package com.dduru.gildongmu.notification.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.dto.response.NotificationReadResponse;
import com.dduru.gildongmu.notification.exception.NotificationAccessDeniedException;
import com.dduru.gildongmu.notification.exception.NotificationNotFoundException;
import com.dduru.gildongmu.notification.repository.NotificationRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 22, 10, 0);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeProvider timeProvider;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(timeProvider.now()).thenReturn(NOW);
        notificationService = new NotificationService(notificationRepository, userRepository, timeProvider);
    }

    @Nested
    @DisplayName("단건 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("삭제된 알림은 읽음 처리할 수 없다")
        void deletedNotificationThrowsNotFound() {
            Notification notification = createUnreadNotification(10L, createUser(1L));
            ReflectionTestUtils.setField(notification, "deletedAt", NOW.minusHours(1));
            when(notificationRepository.getByIdOrThrow(10L)).thenReturn(notification);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                    .isInstanceOf(NotificationNotFoundException.class);
            assertThat(notification.isRead()).isFalse();
            verify(notificationRepository).getByIdOrThrow(10L);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("한 달보다 오래된 알림은 읽음 처리할 수 없다")
        void expiredNotificationThrowsNotFound() {
            Notification notification = createUnreadNotification(10L, createUser(1L));
            ReflectionTestUtils.setField(notification, "createdAt", NOW.minusMonths(1).minusSeconds(1));
            when(notificationRepository.getByIdOrThrow(10L)).thenReturn(notification);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 10L))
                    .isInstanceOf(NotificationNotFoundException.class);
            verify(notificationRepository).getByIdOrThrow(10L);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("정확히 한 달 전 알림은 읽음 처리할 수 있다")
        void monthBoundaryIsIncluded() {
            Notification notification = createUnreadNotification(10L, createUser(1L));
            ReflectionTestUtils.setField(notification, "createdAt", NOW.minusMonths(1));
            when(notificationRepository.getByIdOrThrow(10L)).thenReturn(notification);

            notificationService.markAsRead(1L, 10L);

            verify(notificationRepository).markAsReadByIdAndRecipientId(10L, 1L, NOW.minusMonths(1), NOW);
        }

        @Test
        @DisplayName("미읽음 알림은 엔티티 변경 없이 조건부 UPDATE로 읽음 처리한다")
        void unreadNotificationIsMarkedAsRead() {
            Long userId = 1L;
            Long notificationId = 10L;
            Notification notification = createUnreadNotification(notificationId, createUser(userId));

            when(notificationRepository.getByIdOrThrow(notificationId)).thenReturn(notification);
            when(notificationRepository.markAsReadByIdAndRecipientId(notificationId, userId, NOW.minusMonths(1), NOW))
                    .thenReturn(1);

            NotificationReadResponse response = notificationService.markAsRead(userId, notificationId);

            assertThat(response.success()).isTrue();
            verify(notificationRepository).markAsReadByIdAndRecipientId(notificationId, userId, NOW.minusMonths(1), NOW);
            assertThat(notification.isRead()).isFalse();
            assertThat(notification.getReadAt()).isNull();
        }

        @Test
        @DisplayName("검증 이후 다른 요청이 먼저 처리하여 UPDATE가 0건이어도 성공한다")
        void concurrentUpdateIsIdempotent() {
            Notification notification = createUnreadNotification(10L, createUser(1L));
            when(notificationRepository.getByIdOrThrow(10L)).thenReturn(notification);
            when(notificationRepository.markAsReadByIdAndRecipientId(10L, 1L, NOW.minusMonths(1), NOW))
                    .thenReturn(0);

            assertThat(notificationService.markAsRead(1L, 10L).success()).isTrue();
            verify(notificationRepository).markAsReadByIdAndRecipientId(10L, 1L, NOW.minusMonths(1), NOW);
        }

        @Test
        @DisplayName("이미 읽은 알림을 재요청해도 예외 없이 success=true를 반환한다 (멱등)")
        void alreadyReadNotificationIsIdempotent() {
            Long userId = 1L;
            Long notificationId = 10L;
            Notification notification = createReadNotification(notificationId, createUser(userId));
            LocalDateTime originalReadAt = notification.getReadAt();

            when(notificationRepository.getByIdOrThrow(notificationId)).thenReturn(notification);

            NotificationReadResponse response = notificationService.markAsRead(userId, notificationId);

            assertThat(response.success()).isTrue();
            assertThat(notification.getReadAt()).isEqualTo(originalReadAt); // readAt 변경 없음
            verify(notificationRepository).getByIdOrThrow(notificationId);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("존재하지 않는 알림 ID로 요청하면 NotificationNotFoundException이 발생한다")
        void notFoundNotificationThrowsException() {
            when(notificationRepository.getByIdOrThrow(99L)).thenThrow(NotificationNotFoundException.class);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                    .isInstanceOf(NotificationNotFoundException.class);
        }

        @Test
        @DisplayName("타인의 알림을 읽음 처리하려 하면 NotificationAccessDeniedException이 발생한다")
        void otherUsersNotificationThrowsAccessDeniedException() {
            Long ownerId = 1L;
            Long attackerId = 2L;
            Long notificationId = 10L;
            Notification notification = createUnreadNotification(notificationId, createUser(ownerId));

            when(notificationRepository.getByIdOrThrow(notificationId)).thenReturn(notification);

            assertThatThrownBy(() -> notificationService.markAsRead(attackerId, notificationId))
                    .isInstanceOf(NotificationAccessDeniedException.class);

            assertThat(notification.isRead()).isFalse(); // 상태 변경 없음
            verify(notificationRepository).getByIdOrThrow(notificationId);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        @DisplayName("접근 권한이 없으면 알림 상태는 변경되지 않는다")
        void accessDeniedDoesNotMutateNotificationState() {
            Long ownerId = 1L;
            Long notificationId = 10L;
            Notification notification = createUnreadNotification(notificationId, createUser(ownerId));

            when(notificationRepository.getByIdOrThrow(notificationId)).thenReturn(notification);

            try {
                notificationService.markAsRead(99L, notificationId);
            } catch (NotificationAccessDeniedException ignored) {
            }

            verify(timeProvider, never()).now();
        }
    }

    @Nested
    @DisplayName("전체 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리하면 벌크 업데이트 쿼리를 실행하고 success=true를 반환한다")
        void bulkUpdateIsCalledAndReturnsSuccess() {
            Long userId = 1L;

            NotificationReadResponse response = notificationService.markAllAsRead(userId);

            assertThat(response.success()).isTrue();
            verify(notificationRepository).markAllAsReadByRecipientId(userId, NOW.minusMonths(1), NOW);
        }
    }

    @Test
    @DisplayName("전체 삭제는 본인의 최근 한 달 알림을 대상으로 소프트 삭제 쿼리를 실행한다")
    void deleteAllNotificationsUsesVisibleMonth() {
        notificationService.deleteAllNotifications(1L);

        verify(notificationRepository).softDeleteAllByRecipientId(1L, NOW.minusMonths(1), NOW);
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

    private Notification createUnreadNotification(Long notificationId, User recipient) {
        Notification notification = Notification.create(
                recipient,
                NotificationType.MATCH_APPLIED,
                "[테스트 게시글]에 새로운 참여 신청이 도착했습니다.",
                ResourceType.MATCH,
                1L
        );
        ReflectionTestUtils.setField(notification, "id", notificationId);
        ReflectionTestUtils.setField(notification, "createdAt", NOW.minusDays(1));
        return notification;
    }

    private Notification createReadNotification(Long notificationId, User recipient) {
        Notification notification = createUnreadNotification(notificationId, recipient);
        ReflectionTestUtils.setField(notification, "read", true);
        ReflectionTestUtils.setField(notification, "readAt", NOW.minusHours(1));
        return notification;
    }
}
