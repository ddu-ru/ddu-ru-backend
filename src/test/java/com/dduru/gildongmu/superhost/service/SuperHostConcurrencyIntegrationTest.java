package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.superhost.domain.SuperHostExposure;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostExposureStatus;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketStatus;
import com.dduru.gildongmu.superhost.exception.SuperHostTicketNotFoundException;
import com.dduru.gildongmu.superhost.repository.SuperHostExposureRepository;
import com.dduru.gildongmu.superhost.repository.SuperHostTicketRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({QueryDslConfig.class, SuperHostService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("슈퍼호스트 노출 동시성 통합 테스트")
class SuperHostConcurrencyIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);

    @Autowired private UserRepository userRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private SuperHostTicketRepository ticketRepository;
    @Autowired private SuperHostExposureRepository exposureRepository;
    @Autowired private SuperHostService service;

    @MockitoBean private TimeProvider timeProvider;

    private User user;
    private Destination destination;
    private Post post;

    @BeforeEach
    void setUp() {
        when(timeProvider.now()).thenReturn(NOW);
        user = userRepository.save(User.builder()
                .email("super-host-concurrency@example.com")
                .name("슈퍼호스트")
                .oauthId("super-host-concurrency")
                .oauthType(OauthType.KAKAO)
                .build());
        destination = destinationRepository.save(Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .image("https://example.com/jeju.jpg")
                .build());
        post = postRepository.save(createPost("동시 적용 테스트 게시글"));
    }

    @AfterEach
    void tearDown() {
        exposureRepository.deleteAll();
        ticketRepository.deleteAll();
        postRepository.deleteAll();
        destinationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 게시글에 티켓 두 장을 동시에 적용해도 최종 종료 시각이 6일 연장된다")
    void concurrentApplicationsAccumulateExposureDuration() throws Exception {
        ticketRepository.saveAllAndFlush(List.of(createTicket(), createTicket()));
        var results = executeConcurrently(List.of(post.getId(), post.getId()));

        assertThat(results).containsExactlyInAnyOrder("SUCCESS", "SUCCESS");
        assertThat(exposureRepository.findAll())
                .hasSize(2)
                .allMatch(exposure -> exposure.getStatus() == SuperHostExposureStatus.ACTIVE);
        assertThat(exposureRepository.findAll())
                .extracting(SuperHostExposure::getEndedAt)
                .containsExactlyInAnyOrder(NOW.plusDays(3), NOW.plusDays(6));
        assertThat(exposureRepository.findLatestActiveEndedAtByPostId(
                post.getId(), SuperHostExposureStatus.ACTIVE, NOW
        )).contains(NOW.plusDays(6));
        assertThat(ticketRepository.countByUser_IdAndStatus(user.getId(), SuperHostTicketStatus.USED)).isEqualTo(2);
        assertThat(ticketRepository.countByUser_IdAndStatus(user.getId(), SuperHostTicketStatus.UNUSED)).isZero();

        assertThat(service.getMyStatus(user.getId()).appliedPosts())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.postId()).isEqualTo(post.getId());
                    assertThat(item.superHostEndsAt()).isEqualTo(NOW.plusDays(6));
                });
    }

    @Test
    @DisplayName("서로 다른 게시글에 동시에 요청해도 티켓 한 장은 한 번만 적용된다")
    void concurrentApplicationsToDifferentPostsConsumeSingleTicketOnlyOnce() throws Exception {
        Post anotherPost = postRepository.saveAndFlush(createPost("다른 동시 적용 테스트 게시글"));
        ticketRepository.saveAndFlush(createTicket());
        var results = executeConcurrently(List.of(post.getId(), anotherPost.getId()));

        assertThat(results).containsExactlyInAnyOrder("SUCCESS", "NO_TICKET");
        assertThat(exposureRepository.findAll())
                .singleElement()
                .extracting(SuperHostExposure::getEndedAt)
                .isEqualTo(NOW.plusDays(3));
        assertThat(ticketRepository.countByUser_IdAndStatus(user.getId(), SuperHostTicketStatus.USED)).isEqualTo(1);
        assertThat(ticketRepository.countByUser_IdAndStatus(user.getId(), SuperHostTicketStatus.UNUSED)).isZero();
    }

    @Test
    @DisplayName("온보딩 보상 지급 요청이 동시에 실행돼도 티켓은 3장만 지급된다")
    void concurrentOnboardingRewardsIssueThreeTicketsOnlyOnce() throws Exception {
        int requestCount = 2;
        var ready = new CountDownLatch(requestCount);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);

        try {
            var futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(ignored -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("동시 실행 시작 대기 시간 초과");
                        }
                        service.grantOnboardingRewardTicket(user.getId());
                        return "SUCCESS";
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo("SUCCESS");
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(ticketRepository.countByUser_IdAndStatus(user.getId(), SuperHostTicketStatus.UNUSED))
                .isEqualTo(3);
        assertThat(ticketRepository.findAll())
                .allSatisfy(ticket -> {
                    assertThat(ticket.getSource()).isEqualTo(SuperHostTicketSource.ONBOARDING_SURVEY);
                    assertThat(ticket.getBoostDurationDays()).isEqualTo(3);
                });
    }

    private List<String> executeConcurrently(List<Long> postIds) throws Exception {
        int requestCount = postIds.size();
        var ready = new CountDownLatch(requestCount);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);

        try {
            var futures = postIds.stream()
                    .map(postId -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("동시 실행 시작 대기 시간 초과");
                        }
                        try {
                            service.applyTicketToPost(user.getId(), postId);
                            return "SUCCESS";
                        } catch (SuperHostTicketNotFoundException e) {
                            return "NO_TICKET";
                        }
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private SuperHostTicket createTicket() {
        return SuperHostTicket.create(user, SuperHostTicketSource.ONBOARDING_SURVEY, 3);
    }

    private Post createPost(String title) {
        return Post.createPost(
                user,
                destination,
                title,
                "동일 게시물에 대한 슈퍼호스트 동시 적용을 검증하기 위한 본문입니다.",
                NOW.toLocalDate().plusDays(3),
                NOW.toLocalDate().plusDays(5),
                4,
                NOW.toLocalDate().plusDays(2),
                Gender.U,
                true,
                null,
                null,
                null,
                "[]",
                CompanionType.FULL
        );
    }
}
