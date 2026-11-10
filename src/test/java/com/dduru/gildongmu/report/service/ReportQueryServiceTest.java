package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.exception.ReportNotFoundException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportQueryService 테스트")
class ReportQueryServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportQueryService reportQueryService;

    @Test
    @DisplayName("신고자 본인은 처리 상태를 조회할 수 있다")
    void ownerCanReadStatus() {
        User reporter = User.builder().name("reporter").build();
        User target = User.builder().name("target").build();
        Report report = Report.forUser(reporter, target, ReportReason.OTHER, null);
        when(reportRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(report));

        assertThat(reportQueryService.retrieveStatus(5L, 1L).status()).isEqualTo(ReportStatus.RECEIVED);
    }

    @Test
    @DisplayName("타인의 신고는 조회할 수 없다")
    void hidesOtherUsersReports() {
        assertThatThrownBy(() -> reportQueryService.retrieveStatus(5L, 2L))
                .isInstanceOf(ReportNotFoundException.class);
        verify(reportRepository).findByIdAndUserId(5L, 2L);
    }
}
