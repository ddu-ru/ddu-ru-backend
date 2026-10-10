package com.dduru.gildongmu.notification.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(User recipient, NotificationType type, String body,
                         ResourceType resourceType, Long resourceId) {
        this.recipient = recipient;
        this.type = type;
        this.body = body;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.read = false;
    }

    public static Notification create(
            User recipient,
            NotificationType type,
            String body,
            ResourceType resourceType,
            Long resourceId
    ) {
        return Notification.builder()
                .recipient(recipient)
                .type(type)
                .body(body)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();
    }

    public boolean isNotOwnedBy(Long userId) {
        return !recipient.getId().equals(userId);
    }

    public boolean isUnread() {
        return !read;
    }

    public boolean isVisibleSince(LocalDateTime since) {
        return deletedAt == null && !getCreatedAt().isBefore(since);
    }
}
