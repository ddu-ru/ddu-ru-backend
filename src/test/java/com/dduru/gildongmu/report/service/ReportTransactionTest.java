package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.dto.request.ReportCreateRequest;
import com.dduru.gildongmu.report.exception.DuplicateReportException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({QueryDslConfig.class, ReportService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("신고 접수 트랜잭션 테스트")
class ReportTransactionTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ChatReportAccessValidator chatReportAccessValidator;

    @MockitoBean
    private ReportEvidenceService reportEvidenceService;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("동시에 동일 사용자를 신고해도 미처리 신고는 하나만 저장된다")
    void simultaneousUserReportsCreateOnlyOneOpenReport() throws Exception {
        List<Long> ids = fixture();
        long before = reportRepository.count();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> submit = () -> {
            start.await();
            try {
                reportService.reportUser(ids.get(1), ids.get(0), new ReportCreateRequest(ReportReason.OTHER, null));
                return true;
            } catch (DuplicateReportException e) {
                return false;
            }
        };
        try {
            Future<Boolean> a = executor.submit(submit);
            Future<Boolean> b = executor.submit(submit);
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(reportRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("처리 완료 후 다시 신고할 수 있고 관리자 목록에 표시된다")
    void completedUserReportAllowsNewIncidentAndAdminQueryIncludesUserReports() {
        List<Long> ids = fixture();
        var first = reportService.reportUser(ids.get(1), ids.get(0), new ReportCreateRequest(ReportReason.OTHER, "first"));
        transactionTemplate.executeWithoutResult(status -> reportRepository.findById(first.reportId()).orElseThrow()
                .updateByAdmin(ReportStatus.RESOLVED, userRepository.getByIdOrThrow(ids.get(0)), "reviewed", LocalDateTime.now()));
        var second = reportService.reportUser(ids.get(1), ids.get(0), new ReportCreateRequest(ReportReason.OTHER, "second"));
        assertThat(second.reportId()).isNotEqualTo(first.reportId());
        assertThat(reportRepository.findAllByStatusOptional(ReportStatus.RECEIVED, PageRequest.of(0, 100)).getContent())
                .extracting(report -> report.getId()).contains(second.reportId());
    }

    private List<Long> fixture() {
        return transactionTemplate.execute(status -> {
            User reporter = user("reporter");
            User target = user("target");
            entityManager.persist(reporter);
            entityManager.persist(target);
            return List.of(reporter.getId(), target.getId());
        });
    }

    private User user(String name) {
        String unique = UUID.randomUUID().toString();
        return User.builder().name(name).email(unique + "@example.com").oauthId(unique).oauthType(OauthType.KAKAO).build();
    }
}
