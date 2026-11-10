package com.dduru.gildongmu.report.dto.request;

import com.dduru.gildongmu.report.domain.enums.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @Schema(description = "신고 사유", example = "SPAM_AD")
        @NotNull(message = "신고 사유는 필수입니다")
        ReportReason reason,

        @Schema(description = "신고 상세 내용. 선택 입력이며 최대 200자입니다.", example = "광고성 내용입니다.", nullable = true)
        @Size(max = 200, message = "상세 내용은 200자 이하여야 합니다")
        String description,

        @Schema(description = "신고 증빙 업로드 API에서 받은 evidenceKey. 선택 1장", nullable = true)
        @Size(max = 512)
        String evidenceKey
) {
    public ReportCreateRequest(ReportReason reason, String description) {
        this(reason, description, null);
    }
}
