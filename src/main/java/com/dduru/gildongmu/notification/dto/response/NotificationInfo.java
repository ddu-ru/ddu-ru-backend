package com.dduru.gildongmu.notification.dto.response;

import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 단건 정보")
public record NotificationInfo(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,
        @Schema(description = "알림 타입", example = "MATCH_APPLIED")
        NotificationType type,
        @Schema(description = "알림 제목", example = "매칭 신청 도착")
        String title,
        @Schema(description = "알림 본문", example = "[일본 여행 모집]에 새로운 참여 신청이 도착했습니다.")
        String body,
        @Schema(description = "연결된 리소스 타입", example = "MATCH")
        ResourceType resourceType,
        @Schema(description = "연결된 리소스 ID", example = "10")
        Long resourceId,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "생성 일시", example = "2025-07-01T10:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static NotificationInfo from(Notification notification) {
        return new NotificationInfo(
                notification.getId(),
                notification.getType(),
                notification.getType().getTitle(),
                notification.getBody(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
