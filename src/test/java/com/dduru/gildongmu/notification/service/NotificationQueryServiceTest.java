package com.dduru.gildongmu.notification.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationFilter;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.dto.query.NotificationUnreadCountQueryResult;
import com.dduru.gildongmu.notification.dto.request.NotificationListRequest;
import com.dduru.gildongmu.notification.dto.response.NotificationListResponse;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
import com.dduru.gildongmu.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService 테스트")
class NotificationQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 31, 10, 0);
    private static final LocalDateTime SINCE = LocalDateTime.of(2026, 2, 28, 10, 0);

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TimeProvider timeProvider;
    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @BeforeEach
    void setUp() {
        when(timeProvider.now()).thenReturn(NOW);
    }

    @Test
    @DisplayName("필터 미전달 시 ALL, 크기 20, 달력 기준 한 달 조건으로 조회한다")
    void defaultsUseCalendarMonth() {
        when(notificationRepository.findPageByRecipientId(
                1L, null, SINCE, NotificationFilter.ALL.types(), false, PageRequest.of(0, 21)))
                .thenReturn(List.of());

        NotificationListResponse response = notificationQueryService.getNotifications(
                1L, new NotificationListRequest(null, null, null));

        assertThat(response.notifications()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("미읽음 탭 조건과 커서를 전달하고 다음 페이지 및 표시 제목을 반환한다")
    void unreadFilterAndPagination() {
        when(notificationRepository.findPageByRecipientId(
                1L, 30L, SINCE, NotificationFilter.UNREAD.types(), true, PageRequest.of(0, 3)))
                .thenReturn(List.of(notification(29L), notification(28L), notification(27L)));

        NotificationListResponse response = notificationQueryService.getNotifications(
                1L, new NotificationListRequest(30L, 2, NotificationFilter.UNREAD));

        assertThat(response.notifications()).extracting(item -> item.notificationId()).containsExactly(29L, 28L);
        assertThat(response.notifications()).extracting(item -> item.title()).containsOnly("매칭 신청 도착");
        assertThat(response.nextCursor()).isEqualTo(28L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("타입별 미읽음 수를 전체 및 카테고리별로 합산한다")
    void aggregatesCategoryCounts() {
        when(notificationRepository.countUnreadByType(1L, SINCE)).thenReturn(List.of(
                new NotificationUnreadCountQueryResult(NotificationType.MATCH_APPLIED, 2),
                new NotificationUnreadCountQueryResult(NotificationType.MATCH_APPROVED, 1),
                new NotificationUnreadCountQueryResult(NotificationType.POST_UPDATED, 3),
                new NotificationUnreadCountQueryResult(NotificationType.JOURNEY_NOTICE, 1),
                new NotificationUnreadCountQueryResult(NotificationType.SCHEDULE_UPDATED, 2),
                new NotificationUnreadCountQueryResult(NotificationType.TRIP_UPCOMING, 1)));

        assertThat(notificationQueryService.getUnreadCount(1L))
                .isEqualTo(new UnreadCountResponse(10, 6, 4));
    }

    @Test
    @DisplayName("미읽음 알림이 없으면 모든 카운트가 0이다")
    void emptyCountsAreZero() {
        when(notificationRepository.countUnreadByType(1L, SINCE)).thenReturn(List.of());

        assertThat(notificationQueryService.getUnreadCount(1L))
                .isEqualTo(new UnreadCountResponse(0, 0, 0));
    }

    private Notification notification(Long id) {
        Notification notification = Notification.create(null, NotificationType.MATCH_APPLIED,
                "김철수 님이 매칭을 신청했습니다.", ResourceType.MATCH, 1L);
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", NOW);
        return notification;
    }
}
