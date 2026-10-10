package com.dduru.gildongmu.notification.repository;

import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.dto.query.NotificationUnreadCountQueryResult;
import com.dduru.gildongmu.notification.exception.NotificationNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.id = :userId
              AND n.deletedAt IS NULL
              AND n.createdAt >= :since
              AND n.type IN :types
              AND (:unreadOnly = false OR n.read = false)
              AND (:cursor IS NULL OR n.id < :cursor)
            ORDER BY n.id DESC
            """)
    List<Notification> findPageByRecipientId(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            @Param("since") LocalDateTime since,
            @Param("types") List<NotificationType> types,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable
    );

    default Notification getByIdOrThrow(Long id) {
        return findById(id).orElseThrow(NotificationNotFoundException::new);
    }

    @Query("""
            SELECT new com.dduru.gildongmu.notification.dto.query.NotificationUnreadCountQueryResult(n.type, COUNT(n))
            FROM Notification n
            WHERE n.recipient.id = :userId AND n.read = false
              AND n.deletedAt IS NULL AND n.createdAt >= :since
            GROUP BY n.type
            """)
    List<NotificationUnreadCountQueryResult> countUnreadByType(
            @Param("userId") Long userId, @Param("since") LocalDateTime since);

    // 벌크 UPDATE는 1차 캐시를 거치지 않으므로, 이후 조회 시 stale 상태를 반환하지 않도록 캐시를 비움
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = :now, n.modifiedAt = :now
            WHERE n.id = :notificationId AND n.recipient.id = :userId
              AND n.read = false AND n.deletedAt IS NULL
              AND n.createdAt >= :since
            """)
    int markAsReadByIdAndRecipientId(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = :now, n.modifiedAt = :now
            WHERE n.recipient.id = :userId AND n.read = false
              AND n.deletedAt IS NULL
              AND n.createdAt >= :since AND n.createdAt <= :now
            """)
    int markAllAsReadByRecipientId(
            @Param("userId") Long userId, @Param("since") LocalDateTime since, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Notification n SET n.deletedAt = :now, n.modifiedAt = :now
            WHERE n.recipient.id = :userId AND n.deletedAt IS NULL
              AND n.createdAt >= :since AND n.createdAt <= :now
            """)
    int softDeleteAllByRecipientId(
            @Param("userId") Long userId, @Param("since") LocalDateTime since, @Param("now") LocalDateTime now);

    // cutoff(오늘 자정) 조건: 출발일 변경 시 재발송을 허용하기 위해 과거 기록을 중복으로 보지 않음
    // 삭제된 알림도 발송 이력이므로 중복 발송 검사에 포함
    @Query("""
            SELECT DISTINCT n.recipient.id FROM Notification n
            WHERE n.type = :type
              AND n.resourceType = :resourceType
              AND n.resourceId = :resourceId
              AND n.createdAt >= :cutoff
            """)
    List<Long> findNotifiedRecipientIds(
            @Param("type") NotificationType type,
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceId") Long resourceId,
            @Param("cutoff") LocalDateTime cutoff);
}
