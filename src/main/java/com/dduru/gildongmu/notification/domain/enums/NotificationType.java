package com.dduru.gildongmu.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    MATCH_APPLIED(NotificationCategory.MATCH, "매칭 신청 도착"),
    MATCH_APPROVED(NotificationCategory.MATCH, "매칭 성사"),
    JOURNEY_NOTICE(NotificationCategory.JOURNEY, "공지 등록"),
    SCHEDULE_CREATED(NotificationCategory.JOURNEY, "일정 생성"),
    SCHEDULE_UPDATED(NotificationCategory.JOURNEY, "일정 변경"),
    SCHEDULE_CANCELED(NotificationCategory.JOURNEY, "일정 취소"),
    POST_UPDATED(NotificationCategory.MATCH, "찜한 글 업데이트"),
    TRIP_UPCOMING(NotificationCategory.JOURNEY, "여행 임박");

    private final NotificationCategory category;
    private final String title;
}
