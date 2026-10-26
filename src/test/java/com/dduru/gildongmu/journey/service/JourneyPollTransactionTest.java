package com.dduru.gildongmu.journey.service;

import com.dduru.gildongmu.chat.exception.ChatRoomClosedException;
import com.dduru.gildongmu.chat.service.ChatMessageSendService;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.journey.domain.*;
import com.dduru.gildongmu.journey.dto.request.JourneyPollCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollOptionCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollVoteRequest;
import com.dduru.gildongmu.journey.exception.InvalidJourneyPollException;
import com.dduru.gildongmu.journey.repository.*;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import({QueryDslConfig.class, JourneyPollService.class, JourneyPollQueryService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JourneyPollTransactionTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    @Autowired private JourneyPollService service;
    @Autowired private JourneyPollQueryService queryService;
    @Autowired private JourneyPollRepository pollRepository;
    @Autowired private JourneyPollOptionRepository optionRepository;
    @Autowired private JourneyPollVoteRepository voteRepository;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private ChatMessageSendService chatMessageSendService;
    @MockitoBean private TimeProvider timeProvider;
    @MockitoBean private ProfileImageResolver profileImageResolver;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        when(timeProvider.now()).thenReturn(NOW);
    }

    @Test
    void chatFailureRollsBackPollAndOptionsTogether() {
        Fixture fixture = fixture(2);
        long beforePolls = pollRepository.count();
        long beforeOptions = optionRepository.count();
        doThrow(new ChatRoomClosedException()).when(chatMessageSendService).publishPoll(any(), any());
        assertThatThrownBy(() -> service.createPoll(fixture.journeyId(), fixture.userId(),
                new JourneyPollCreateRequest("New poll", List.of("A", "B"), false, false, false, NOW.plusDays(1))))
                .isInstanceOf(ChatRoomClosedException.class);
        assertThat(pollRepository.count()).isEqualTo(beforePolls);
        assertThat(optionRepository.count()).isEqualTo(beforeOptions);
    }

    @Test
    void concurrentFirstVotesAndRevotesLeaveOneSelectionForSingleChoicePoll() throws Exception {
        Fixture fixture = fixture(2);
        race(() -> service.vote(fixture.journeyId(), fixture.pollId(), fixture.userId(),
                        new JourneyPollVoteRequest(List.of(fixture.optionIds().get(0)))),
                () -> service.vote(fixture.journeyId(), fixture.pollId(), fixture.userId(),
                        new JourneyPollVoteRequest(List.of(fixture.optionIds().get(1)))));
        assertThat(voteRepository.findSelectionsByPollIdsAndUserId(List.of(fixture.pollId()), fixture.userId())).hasSize(1);
        var detail = queryService.retrievePoll(fixture.journeyId(), fixture.pollId(), fixture.userId());
        assertThat(detail.poll().voterCount()).isEqualTo(1);
        assertThat(detail.poll().options().stream().mapToLong(option -> option.voteCount()).sum()).isEqualTo(1);
    }

    @Test
    void concurrentOptionAdditionsCannotExceedLimit() throws Exception {
        Fixture fixture = fixture(9);
        var results = race(
                () -> addOption(fixture, "Tenth A"),
                () -> addOption(fixture, "Tenth B"));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(optionRepository.findByPollIdOrderByDisplayOrderAsc(fixture.pollId())).hasSize(10);
    }

    private boolean addOption(Fixture fixture, String content) {
        try {
            service.addOption(fixture.journeyId(), fixture.pollId(), fixture.userId(), new JourneyPollOptionCreateRequest(content));
            return true;
        } catch (InvalidJourneyPollException exception) {
            return false;
        }
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> a = executor.submit(() -> { start.await(); return first.call(); });
            Future<T> b = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Fixture fixture(int optionCount) {
        return transaction.execute(status -> {
            String unique = UUID.randomUUID().toString();
            User user = User.builder().name("host").email(unique + "@example.com")
                    .oauthType(OauthType.KAKAO).oauthId(unique).build();
            em.persist(user);
            Destination destination = Destination.builder().countryCode("KR").countryName("Korea").city(unique).build();
            em.persist(destination);
            Post post = Post.createPost(user, destination, "Seoul trip", "A sufficiently long content for this post.",
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 4, LocalDate.now().plusDays(1),
                    Gender.U, true, null, null, null, "[]", null);
            em.persist(post);
            Journey journey = Journey.create(post);
            em.persist(journey);
            em.persist(JourneyMember.createHost(journey, user, NOW));
            JourneyPoll poll = JourneyPoll.create(journey, user, "Dinner", false, true, true, NOW.plusDays(1), NOW);
            em.persist(poll);
            List<Long> optionIds = new java.util.ArrayList<>();
            for (int index = 0; index < optionCount; index++) {
                JourneyPollOption option = JourneyPollOption.create(poll, "Option " + index, index);
                em.persist(option);
                optionIds.add(option.getId());
            }
            return new Fixture(journey.getId(), user.getId(), poll.getId(), optionIds);
        });
    }

    private record Fixture(Long journeyId, Long userId, Long pollId, List<Long> optionIds) {
    }
}
