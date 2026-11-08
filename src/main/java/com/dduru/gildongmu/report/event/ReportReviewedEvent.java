package com.dduru.gildongmu.report.event;

import com.dduru.gildongmu.report.domain.enums.ReportStatus;

public record ReportReviewedEvent(Long reportId, Long reporterId, ReportStatus status) {
}
