package com.dduru.gildongmu.notification.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationFilter;
import com.dduru.gildongmu.notification.dto.request.NotificationListRequest;
import com.dduru.gildongmu.notification.dto.response.NotificationListResponse;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
import com.dduru.gildongmu.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final TimeProvider timeProvider;

    public NotificationListResponse getNotifications(Long userId, NotificationListRequest request) {
        int size = request.sizeOrDefault();
        NotificationFilter filter = request.filter();
        List<Notification> fetched = notificationRepository.findPageByRecipientId(
                userId, request.cursor(), timeProvider.now().minusMonths(1),
                filter.types(), filter.isUnreadOnly(), PageRequest.of(0, size + 1)
        );
        return NotificationListResponse.of(fetched, size);
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        return UnreadCountResponse.from(
                notificationRepository.countUnreadByType(userId, timeProvider.now().minusMonths(1)));
    }
}
