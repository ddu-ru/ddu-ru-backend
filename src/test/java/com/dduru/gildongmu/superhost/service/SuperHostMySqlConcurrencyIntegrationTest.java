package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.exception.SuperHostTicketNotFoundException;
import com.dduru.gildongmu.superhost.repository.SuperHostTicketRepository;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestRunner;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestStatus;
import com.dduru.gildongmu.support.mysql.MySqlIntegrationTestSupport;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("슈퍼호스트 티켓 MySQL 동시성 통합 테스트")
class SuperHostMySqlConcurrencyIntegrationTest extends MySqlIntegrationTestSupport {

    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    @Autowired private SuperHostService superHostService;
    @Autowired private UserRepository userRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private SuperHostTicketRepository ticketRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("티켓 한 장을 서로 다른 게시글에 동시에 적용해도 한 번만 소비한다")
    void concurrentApplicationsToDifferentPostsConsumeSingleTicketOnce() {
        TicketFixture fixture = createFixture();

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
                .extracting("status")
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
            Long exposedPostId = jdbcTemplate.queryForObject(
                    "SELECT post_id FROM super_host_exposures WHERE ticket_id = ?", Long.class, fixture.ticketId()
            );
            assertThat(exposedPostId).isIn(fixture.firstPostId(), fixture.secondPostId());
            return null;
        });
    }

    private TicketFixture createFixture() {
        return committed(() -> {
            int sequence = FIXTURE_SEQUENCE.incrementAndGet();
            String key = "mysql-ticket-" + sequence;
            User user = userRepository.save(User.builder()
                    .email(key + "@example.com")
                    .name(key)
                    .oauthId(key)
                    .oauthType(OauthType.KAKAO)
                    .build());
            Destination destination = destinationRepository.save(Destination.builder()
                    .countryCode("KR")
                    .countryName("대한민국")
                    .city("부산-" + sequence)
                    .image("https://example.com/busan-" + sequence + ".jpg")
                    .build());
            Post first = postRepository.save(post(user, destination, "첫 번째 티켓 검증 " + sequence));
            Post second = postRepository.save(post(user, destination, "두 번째 티켓 검증 " + sequence));
            SuperHostTicket ticket = ticketRepository.save(SuperHostTicket.create(
                    user, SuperHostTicketSource.ONBOARDING_SURVEY, 3
            ));
            return new TicketFixture(user.getId(), first.getId(), second.getId(), ticket.getId());
        });
    }

    private static Post post(User user, Destination destination, String title) {
        LocalDate today = LocalDate.now();
        return Post.createPost(
                user, destination, title, "단일 티켓의 MySQL 동시 사용을 검증하기 위한 충분한 길이의 본문입니다.",
                today.plusDays(10), today.plusDays(12), 4, today.plusDays(5),
                Gender.U, true, null, null, null, "[]", CompanionType.FULL
        );
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return value == null ? 0 : value;
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record TicketFixture(Long userId, Long firstPostId, Long secondPostId, Long ticketId) {
    }
}
