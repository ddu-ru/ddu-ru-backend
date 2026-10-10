package com.dduru.gildongmu.notification.repository;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationFilter;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("NotificationRepository 테스트")
class NotificationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 31, 10, 0);
    private static final LocalDateTime SINCE = NOW.minusMonths(1);

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User recipient;
    private User otherRecipient;

    @BeforeEach
    void setUp() {
        recipient = persistUser("recipient");
        otherRecipient = persistUser("other");
    }

    @Test
    @DisplayName("목록은 본인의 최근 한 달 미삭제 알림만 조회하며 경계 시각을 포함한다")
    void listExcludesOldDeletedAndOtherUsersNotifications() {
        Long boundaryId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE);
        Long recentId = persistNotification(recipient, NotificationType.SCHEDULE_CREATED, NOW);
        persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE.minusSeconds(1));
        Long deletedId = persistNotification(recipient, NotificationType.POST_UPDATED, NOW);
        deleteNotification(deletedId);
        persistNotification(otherRecipient, NotificationType.MATCH_APPLIED, NOW);

        assertThat(page(NotificationFilter.ALL, null, 20)).extracting(Notification::getId)
                .containsExactly(recentId, boundaryId);
        assertThat(UnreadCountResponse.from(notificationRepository.countUnreadByType(recipient.getId(), SINCE)))
                .isEqualTo(new UnreadCountResponse(2, 1, 1));
    }

    @Test
    @DisplayName("모집·매칭과 여정 탭은 리소스 타입이 아닌 알림 타입으로 분류한다")
    void categoriesCoverEveryNotificationType() {
        for (NotificationType type : NotificationType.values()) {
            persistNotification(recipient, type, NOW);
        }

        assertThat(page(NotificationFilter.MATCH, null, 20)).extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.MATCH_APPLIED, NotificationType.MATCH_APPROVED,
                        NotificationType.POST_UPDATED);
        assertThat(page(NotificationFilter.JOURNEY, null, 20)).extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.JOURNEY_NOTICE, NotificationType.SCHEDULE_CREATED,
                        NotificationType.SCHEDULE_UPDATED, NotificationType.SCHEDULE_CANCELED, NotificationType.TRIP_UPCOMING);
        assertThat(UnreadCountResponse.from(notificationRepository.countUnreadByType(recipient.getId(), SINCE)))
                .isEqualTo(new UnreadCountResponse(8, 3, 5));
    }

    @Test
    @DisplayName("미읽음 탭과 카운트에서는 읽은 알림을 제외한다")
    void unreadFilterAndCountsExcludeReadNotifications() {
        Long unreadId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);
        Long readId = persistNotification(recipient, NotificationType.SCHEDULE_UPDATED, NOW);
        notificationRepository.markAsReadByIdAndRecipientId(readId, recipient.getId(), SINCE, NOW);

        assertThat(page(NotificationFilter.UNREAD, null, 20)).extracting(Notification::getId)
                .containsExactly(unreadId);
        assertThat(page(NotificationFilter.ALL, null, 20)).hasSize(2);
        assertThat(UnreadCountResponse.from(notificationRepository.countUnreadByType(recipient.getId(), SINCE)))
                .isEqualTo(new UnreadCountResponse(1, 1, 0));
    }

    @Test
    @DisplayName("커서 알림이 삭제되어도 ID 내림차순으로 다음 페이지를 조회한다")
    void cursorPaginationSurvivesCursorDeletion() {
        Long firstId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);
        Long secondId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);
        Long thirdId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);

        assertThat(page(NotificationFilter.ALL, null, 2)).extracting(Notification::getId)
                .containsExactly(thirdId, secondId);
        deleteNotification(secondId);

        assertThat(page(NotificationFilter.ALL, secondId, 2)).extracting(Notification::getId)
                .containsExactly(firstId);
    }

    @Test
    @DisplayName("전체 읽음은 최근 한 달의 본인 미삭제 알림에만 적용하며 멱등이다")
    void bulkReadIsScopedAndIdempotent() {
        Long targetId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE);
        Long oldId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE.minusSeconds(1));
        Long otherId = persistNotification(otherRecipient, NotificationType.MATCH_APPLIED, NOW);
        Long deletedId = persistNotification(recipient, NotificationType.SCHEDULE_CREATED, NOW);
        deleteNotification(deletedId);
        Long laterId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW.plusMinutes(2));

        assertThat(notificationRepository.markAllAsReadByRecipientId(recipient.getId(), SINCE, NOW)).isEqualTo(1);
        assertThat(notificationRepository.getByIdOrThrow(targetId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.markAllAsReadByRecipientId(recipient.getId(), SINCE, NOW.plusMinutes(1))).isZero();
        assertThat(notificationRepository.getByIdOrThrow(targetId).getReadAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(targetId).getModifiedAt()).isEqualTo(NOW);
        for (Long id : List.of(oldId, otherId, deletedId, laterId)) {
            assertThat(notificationRepository.getByIdOrThrow(id).isRead()).isFalse();
        }
    }

    @Test
    @DisplayName("전체 삭제는 본인의 최근 한 달만 숨기고 발송 이력 및 다른 사용자 알림을 보존한다")
    void softDeletePreservesHistoryAndOtherUsersNotifications() {
        Long matchId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE);
        Long tripId = persistNotification(recipient, NotificationType.TRIP_UPCOMING, NOW.minusHours(1));
        Long oldId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE.minusSeconds(1));
        Long otherId = persistNotification(otherRecipient, NotificationType.TRIP_UPCOMING, NOW);

        assertThat(notificationRepository.softDeleteAllByRecipientId(recipient.getId(), SINCE, NOW)).isEqualTo(2);
        assertThat(notificationRepository.getByIdOrThrow(matchId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(tripId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.softDeleteAllByRecipientId(recipient.getId(), SINCE, NOW.plusMinutes(1))).isZero();
        assertThat(page(NotificationFilter.ALL, null, 20)).isEmpty();
        assertThat(notificationRepository.countUnreadByType(recipient.getId(), SINCE)).isEmpty();
        assertThat(notificationRepository.getByIdOrThrow(matchId).getDeletedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(tripId).getDeletedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(matchId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(tripId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(oldId).getDeletedAt()).isNull();
        assertThat(notificationRepository.getByIdOrThrow(otherId).getDeletedAt()).isNull();
        assertThat(notificationRepository.findNotifiedRecipientIds(NotificationType.TRIP_UPCOMING,
                ResourceType.JOURNEY, 1L, NOW.toLocalDate().atStartOfDay()))
                .containsExactlyInAnyOrder(recipient.getId(), otherRecipient.getId());
    }

    @Test
    @DisplayName("단건 읽음은 대상 알림만 변경하며 반복 요청 시 최초 읽음 시각을 유지한다")
    void singleReadIsScopedAndIdempotent() {
        Long targetId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE);
        Long untouchedId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);

        assertThat(notificationRepository.markAsReadByIdAndRecipientId(targetId, recipient.getId(), SINCE, NOW))
                .isEqualTo(1);
        assertThat(notificationRepository.getByIdOrThrow(targetId).getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.markAsReadByIdAndRecipientId(targetId, recipient.getId(), SINCE, NOW.plusMinutes(1)))
                .isZero();

        Notification notification = notificationRepository.getByIdOrThrow(targetId);
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(NOW);
        assertThat(notification.getModifiedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(untouchedId).isRead()).isFalse();
    }

    @Test
    @DisplayName("읽은 알림을 삭제하면 읽음 시각은 유지하고 수정 시각은 삭제 시각으로 갱신한다")
    void deletionAfterReadUpdatesOnlyLastModifiedTime() {
        Long id = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE);
        LocalDateTime deletedAt = NOW.plusMinutes(1);

        notificationRepository.markAsReadByIdAndRecipientId(id, recipient.getId(), SINCE, NOW);
        notificationRepository.softDeleteAllByRecipientId(recipient.getId(), SINCE, deletedAt);
        assertThat(notificationRepository.markAsReadByIdAndRecipientId(id, recipient.getId(), SINCE, NOW.plusMinutes(2)))
                .isZero();

        Notification notification = notificationRepository.getByIdOrThrow(id);
        assertThat(notification.getReadAt()).isEqualTo(NOW);
        assertThat(notification.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(notification.getModifiedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("단건 읽음 쿼리는 타인·삭제·기간 만료·없는 알림을 변경하지 않는다")
    void singleReadRejectsIneligibleNotifications() {
        Long oldId = persistNotification(recipient, NotificationType.MATCH_APPLIED, SINCE.minusSeconds(1));
        Long otherId = persistNotification(otherRecipient, NotificationType.MATCH_APPLIED, NOW);
        Long deletedId = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);
        deleteNotification(deletedId);

        for (Long id : List.of(oldId, otherId, deletedId, Long.MAX_VALUE)) {
            assertThat(notificationRepository.markAsReadByIdAndRecipientId(id, recipient.getId(), SINCE, NOW)).isZero();
        }
        for (Long id : List.of(oldId, otherId, deletedId)) {
            assertThat(notificationRepository.getByIdOrThrow(id).isRead()).isFalse();
            assertThat(notificationRepository.getByIdOrThrow(id).getReadAt()).isNull();
        }
    }

    @Test
    @DisplayName("삭제와 읽음 처리가 겹쳐도 읽음 처리로 삭제 상태가 복구되지 않는다")
    void staleReadDoesNotRestoreDeletedNotification() {
        Long id = persistNotification(recipient, NotificationType.MATCH_APPLIED, NOW);
        Notification loaded = notificationRepository.getByIdOrThrow(id);
        // 다른 트랜잭션에서 발생한 삭제를 흉내 내되 기존 영속성 컨텍스트는 유지한다.
        entityManager.getEntityManager().createNativeQuery("UPDATE notifications SET deleted_at = :now WHERE id = :id")
                .setParameter("now", NOW).setParameter("id", id).executeUpdate();

        assertThat(loaded.getDeletedAt()).isNull();
        assertThat(notificationRepository.markAsReadByIdAndRecipientId(id, recipient.getId(), SINCE, NOW)).isZero();
        entityManager.flush();
        entityManager.clear();

        assertThat(notificationRepository.getByIdOrThrow(id).getDeletedAt()).isEqualTo(NOW);
        assertThat(notificationRepository.getByIdOrThrow(id).isRead()).isFalse();
        assertThat(notificationRepository.getByIdOrThrow(id).getReadAt()).isNull();
        assertThat(page(NotificationFilter.ALL, null, 20)).isEmpty();
    }

    private List<Notification> page(NotificationFilter filter, Long cursor, int size) {
        return notificationRepository.findPageByRecipientId(recipient.getId(), cursor, SINCE,
                filter.types(), filter.isUnreadOnly(), PageRequest.of(0, size));
    }

    private User persistUser(String name) {
        return entityManager.persist(User.builder().email(name + "@example.com").name(name)
                .oauthId(name).oauthType(OauthType.KAKAO).build());
    }

    private Long persistNotification(User user, NotificationType type, LocalDateTime createdAt) {
        Notification notification = entityManager.persistAndFlush(
                Notification.create(user, type, "알림 본문", ResourceType.JOURNEY, 1L));
        entityManager.getEntityManager().createNativeQuery("UPDATE notifications SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt).setParameter("id", notification.getId()).executeUpdate();
        entityManager.clear();
        return notification.getId();
    }

    private void deleteNotification(Long id) {
        entityManager.getEntityManager().createNativeQuery("UPDATE notifications SET deleted_at = :now WHERE id = :id")
                .setParameter("now", NOW).setParameter("id", id).executeUpdate();
        entityManager.clear();
    }
}
