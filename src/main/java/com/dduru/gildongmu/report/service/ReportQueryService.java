package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.dto.response.ReportStatusResponse;
import com.dduru.gildongmu.report.exception.ReportNotFoundException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryService {

    private final ReportRepository reportRepository;

    public ReportStatusResponse retrieveStatus(Long reportId, Long userId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(ReportNotFoundException::new);
        return ReportStatusResponse.from(report);
    }
}
