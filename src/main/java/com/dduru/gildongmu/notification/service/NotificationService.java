package com.dduru.gildongmu.notification.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.dto.response.NotificationReadResponse;
import com.dduru.gildongmu.notification.exception.NotificationAccessDeniedException;
import com.dduru.gildongmu.notification.exception.NotificationNotFoundException;
import com.dduru.gildongmu.notification.repository.NotificationRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;

    public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.getByIdOrThrow(notificationId);

        if (notification.isNotOwnedBy(userId)) {
            throw new NotificationAccessDeniedException();
        }

        LocalDateTime now = timeProvider.now();
        if (!notification.isVisibleSince(now.minusMonths(1))) {
            throw new NotificationNotFoundException();
        }
        if (notification.isUnread()) {
            notificationRepository.markAsReadByIdAndRecipientId(notificationId, userId, now.minusMonths(1), now);
        }

        return NotificationReadResponse.ok();
    }

    public NotificationReadResponse markAllAsRead(Long userId) {
        LocalDateTime now = timeProvider.now();
        notificationRepository.markAllAsReadByRecipientId(userId, now.minusMonths(1), now);
        return NotificationReadResponse.ok();
    }

    public void deleteAllNotifications(Long userId) {
        LocalDateTime now = timeProvider.now();
        notificationRepository.softDeleteAllByRecipientId(userId, now.minusMonths(1), now);
    }

    public void updateNotificationSettings(Long userId, boolean enabled) {
        User user = userRepository.getByIdOrThrow(userId);
        user.updateNotificationEnabled(enabled);
    }
}
