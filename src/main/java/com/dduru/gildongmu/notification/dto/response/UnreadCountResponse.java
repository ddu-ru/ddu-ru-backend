package com.dduru.gildongmu.notification.dto.response;

import com.dduru.gildongmu.notification.domain.enums.NotificationCategory;
import com.dduru.gildongmu.notification.dto.query.NotificationUnreadCountQueryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "안 읽은 알림 개수 응답")
public record UnreadCountResponse(
        @Schema(description = "최근 한 달의 삭제되지 않은 전체 미읽음 개수", example = "3")
        long unreadCount,
        @Schema(description = "모집·매칭 미읽음 개수", example = "2")
        long matchUnreadCount,
        @Schema(description = "여정 미읽음 개수", example = "1")
        long journeyUnreadCount
) {
    public static UnreadCountResponse from(List<NotificationUnreadCountQueryResult> counts) {
        long total = counts.stream().mapToLong(NotificationUnreadCountQueryResult::count).sum();
        return new UnreadCountResponse(total,
                countByCategory(counts, NotificationCategory.MATCH),
                countByCategory(counts, NotificationCategory.JOURNEY));
    }

    private static long countByCategory(List<NotificationUnreadCountQueryResult> counts, NotificationCategory category) {
        return counts.stream()
                .filter(count -> count.type().getCategory() == category)
                .mapToLong(NotificationUnreadCountQueryResult::count)
                .sum();
    }
}
