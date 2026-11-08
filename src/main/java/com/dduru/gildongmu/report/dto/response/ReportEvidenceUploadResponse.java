package com.dduru.gildongmu.report.dto.response;

public record ReportEvidenceUploadResponse(
        String presignedUrl,
        String evidenceKey
) {
}
