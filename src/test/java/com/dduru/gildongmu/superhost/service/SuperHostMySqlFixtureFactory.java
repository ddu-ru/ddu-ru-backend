package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.repository.SuperHostTicketRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SuperHostMySqlFixtureFactory {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final PostRepository postRepository;
    private final SuperHostTicketRepository ticketRepository;
    private final TimeProvider timeProvider;
    private final PlatformTransactionManager transactionManager;

    public SuperHostMySqlFixtureFactory(
            UserRepository userRepository,
            DestinationRepository destinationRepository,
            PostRepository postRepository,
            SuperHostTicketRepository ticketRepository,
            TimeProvider timeProvider,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.destinationRepository = destinationRepository;
        this.postRepository = postRepository;
        this.ticketRepository = ticketRepository;
        this.timeProvider = timeProvider;
        this.transactionManager = transactionManager;
    }

    TicketFixture ticket() {
        return committed(() -> {
            int sequence = SEQUENCE.incrementAndGet();
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

    private <T> T committed(Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> work.get());
    }

    private Post post(User user, Destination destination, String title) {
        var today = timeProvider.today();
        return Post.createPost(
                user, destination, title,
                "단일 티켓의 MySQL 동시 사용을 검증하기 위한 충분한 길이의 본문입니다.",
                today.plusDays(10), today.plusDays(12), 4, today.plusDays(5),
                Gender.U, true, null, null, null, "[]", CompanionType.FULL
        );
    }

    record TicketFixture(Long userId, Long firstPostId, Long secondPostId, Long ticketId) {
    }
}
