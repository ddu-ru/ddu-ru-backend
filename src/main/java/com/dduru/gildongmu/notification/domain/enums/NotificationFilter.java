package com.dduru.gildongmu.notification.domain.enums;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public enum NotificationFilter {
    ALL(null),
    UNREAD(null),
    MATCH(NotificationCategory.MATCH),
    JOURNEY(NotificationCategory.JOURNEY);

    private final NotificationCategory category;

    public List<NotificationType> types() {
        return Arrays.stream(NotificationType.values())
                .filter(type -> category == null || type.getCategory() == category)
                .toList();
    }

    public boolean isUnreadOnly() {
        return this == UNREAD;
    }
}
