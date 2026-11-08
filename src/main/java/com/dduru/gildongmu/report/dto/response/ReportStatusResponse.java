package com.dduru.gildongmu.report.dto.response;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;

import java.time.LocalDateTime;

public record ReportStatusResponse(
        Long reportId,
        ReportTargetType targetType,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static ReportStatusResponse from(Report report) {
        return new ReportStatusResponse(
                report.getId(),
                report.getTargetType(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewedAt()
        );
    }
}
