package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.superhost.exception.SuperHostTicketNotFoundException;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestRunner;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestResult;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestStatus;
import com.dduru.gildongmu.support.mysql.MySqlIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.dduru.gildongmu.support.concurrency.ConcurrentRequestRunner.hasCause;

@DisplayName("슈퍼호스트 티켓 MySQL 동시성 통합 테스트")
@Import(SuperHostMySqlFixtureFactory.class)
class SuperHostMySqlConcurrencyIntegrationTest extends MySqlIntegrationTestSupport {

    @Autowired private SuperHostService superHostService;
    @Autowired private SuperHostMySqlFixtureFactory fixtures;

    @Test
    @DisplayName("티켓 한 장을 서로 다른 게시글에 동시에 적용해도 한 번만 소비한다")
    void concurrentApplicationsToDifferentPostsConsumeSingleTicketOnce() {
        var fixture = fixtures.ticket();

        var results = ConcurrentRequestRunner.run(
                List.of(
                        () -> superHostService.applyTicketToPost(fixture.userId(), fixture.firstPostId()),
                        () -> superHostService.applyTicketToPost(fixture.userId(), fixture.secondPostId())
                ),
                failure -> hasCause(failure, SuperHostTicketNotFoundException.class)
        );

        assertThat(results).filteredOn(result -> result.status() == ConcurrentRequestStatus.SUCCESS).hasSize(1);
        assertThat(results).filteredOn(result -> result.status() != ConcurrentRequestStatus.SUCCESS)
                .singleElement()
                .extracting(ConcurrentRequestResult::status)
                .isIn(ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT, ConcurrentRequestStatus.DEADLOCK);
        assertThat(results).allMatch(result -> result.status() != ConcurrentRequestStatus.LOCK_TIMEOUT);
        assertThat(results).allMatch(result -> result.status() != ConcurrentRequestStatus.UNEXPECTED_FAILURE);

        queryCommitted(() -> {
            assertThat(count("""
                    SELECT COUNT(*) FROM super_host_tickets
                    WHERE id = ? AND status = 'USED' AND used_at IS NOT NULL
                    """, fixture.ticketId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM super_host_tickets WHERE user_id = ? AND status = 'UNUSED'",
                    fixture.userId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM super_host_exposures WHERE ticket_id = ?",
                    fixture.ticketId())).isEqualTo(1);
            Long exposedPostId = longValue(
                    "SELECT post_id FROM super_host_exposures WHERE ticket_id = ?", fixture.ticketId());
            assertThat(exposedPostId).isIn(fixture.firstPostId(), fixture.secondPostId());
            return null;
        });
    }

}
