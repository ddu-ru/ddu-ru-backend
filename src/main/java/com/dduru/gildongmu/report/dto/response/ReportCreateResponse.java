package com.dduru.gildongmu.report.dto.response;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReportCreateResponse(
        @Schema(description = "생성된 신고 ID", example = "1")
        Long reportId,
        @Schema(description = "신고 처리 상태", example = "RECEIVED")
        ReportStatus status
) {
    public static ReportCreateResponse from(Report report) {
        return new ReportCreateResponse(
                report.getId(),
                report.getStatus()
        );
    }
}
