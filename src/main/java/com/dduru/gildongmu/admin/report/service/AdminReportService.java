package com.dduru.gildongmu.admin.report.service;

import com.dduru.gildongmu.admin.report.dto.request.AdminReportUpdateRequest;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportListResponse;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportResponse;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.event.ReportReviewedEvent;
import com.dduru.gildongmu.report.exception.ReportNotFoundException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AdminReportListResponse findAll(ReportStatus status, Pageable pageable) {
        Page<Report> page = reportRepository.findAllByStatusOptional(status, pageable);
        return AdminReportListResponse.from(page);
    }

    @Transactional
    public AdminReportResponse update(Long reportId, Long reviewerId, AdminReportUpdateRequest request) {
        Report report = reportRepository.findByIdWithLock(reportId)
                .orElseThrow(ReportNotFoundException::new);
        User reviewer = userRepository.getByIdOrThrow(reviewerId);
        ReportStatus previousStatus = report.getStatus();
        report.updateByAdmin(request.status(), reviewer, request.reviewNote(), timeProvider.now());
        if (previousStatus != request.status()
                && (request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.REJECTED)) {
            eventPublisher.publishEvent(new ReportReviewedEvent(reportId, report.getUser().getId(), request.status()));
        }
        log.info("신고 처리 완료 - reportId={}, status={}, reviewerId={}", reportId, request.status(), reviewerId);
        return AdminReportResponse.from(report);
    }
}
