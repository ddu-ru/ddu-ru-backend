package com.dduru.gildongmu.admin.report.dto.response;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long id,
        Long postId,
        String postTitle,
        Long reporterId,
        String reporterName,
        ReportReason reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt,
        Long reviewerId,
        String reviewerName,
        LocalDateTime reviewedAt,
        String reviewNote,
        ReportTargetType targetType,
        Long reportedUserId,
        Long chatRoomId,
        Long messageId,
        String targetSnapshot,
        boolean hasEvidence
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getPost() == null ? null : report.getPost().getId(),
                report.getPost() == null ? null : report.getPost().getTitle(),
                report.getUser().getId(),
                report.getUser().getName(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewer() != null ? report.getReviewer().getId() : null,
                report.getReviewer() != null ? report.getReviewer().getName() : null,
                report.getReviewedAt(),
                report.getReviewNote(),
                report.getTargetType(),
                report.getReportedUser() == null ? null : report.getReportedUser().getId(),
                report.getChatRoom() == null ? null : report.getChatRoom().getId(),
                report.getMessage() == null ? null : report.getMessage().getId(),
                report.getTargetSnapshot(),
                report.getEvidenceKey() != null
        );
    }
}
