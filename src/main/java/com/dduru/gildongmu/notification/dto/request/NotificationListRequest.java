package com.dduru.gildongmu.notification.dto.request;

import com.dduru.gildongmu.notification.domain.enums.NotificationFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Schema(description = "알림 목록 조회 요청")
public record NotificationListRequest(
        @Schema(description = "마지막으로 조회한 알림 ID (첫 요청 시 생략)", example = "42")
        @Positive(message = "cursor는 1 이상이어야 합니다.")
        Long cursor,
        @Schema(description = "조회 개수 (기본값 20, 최대 50)", example = "20")
        @Min(value = 1, message = "size는 1 이상 50 이하로 입력해야 합니다.")
        @Max(value = 50, message = "size는 1 이상 50 이하로 입력해야 합니다.")
        Integer size,
        @Schema(description = "알림 탭. ALL: 전체, UNREAD: 읽지 않음, MATCH: 모집·매칭, JOURNEY: 여정. 기본값 ALL", example = "ALL")
        NotificationFilter filter
) {
    public static final int DEFAULT_SIZE = 20;

    public NotificationListRequest {
        if (filter == null) filter = NotificationFilter.ALL;
    }

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
