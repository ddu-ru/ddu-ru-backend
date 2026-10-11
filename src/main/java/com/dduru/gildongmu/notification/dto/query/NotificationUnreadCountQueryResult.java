package com.dduru.gildongmu.notification.dto.query;

import com.dduru.gildongmu.notification.domain.enums.NotificationType;

public record NotificationUnreadCountQueryResult(NotificationType type, long count) {
}
