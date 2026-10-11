package com.dduru.gildongmu.notification.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.notification.dto.request.NotificationListRequest;
import com.dduru.gildongmu.notification.dto.request.NotificationSettingsRequest;
import com.dduru.gildongmu.notification.dto.response.NotificationListResponse;
import com.dduru.gildongmu.notification.dto.response.NotificationReadResponse;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
import com.dduru.gildongmu.notification.service.NotificationQueryService;
import com.dduru.gildongmu.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationApiDocs {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<NotificationListResponse>> getNotifications(
            @CurrentUser Long userId,
            @Valid @ModelAttribute NotificationListRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(notificationQueryService.getNotifications(userId, request)));
    }

    @Override
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResult<UnreadCountResponse>> getUnreadCount(
            @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResult.ok(notificationQueryService.getUnreadCount(userId)));
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResult<NotificationReadResponse>> markAsRead(
            @CurrentUser Long userId,
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(ApiResult.ok(notificationService.markAsRead(userId, notificationId)));
    }

    @Override
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResult<NotificationReadResponse>> markAllAsRead(
            @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResult.ok(notificationService.markAllAsRead(userId)));
    }

    @Override
    @DeleteMapping
    public ResponseEntity<ApiResult<Void>> deleteAllNotifications(@CurrentUser Long userId) {
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/settings")
    public ResponseEntity<ApiResult<Void>> updateNotificationSettings(
            @CurrentUser Long userId,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        notificationService.updateNotificationSettings(userId, request.enabled());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.noContent());
    }
}
