package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyMember;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ParticipationMySqlFixtureFactory {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final PostRepository postRepository;
    private final ParticipationRepository participationRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyMemberRepository journeyMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final TimeProvider timeProvider;
    private final PlatformTransactionManager transactionManager;

    public ParticipationMySqlFixtureFactory(
            UserRepository userRepository,
            DestinationRepository destinationRepository,
            PostRepository postRepository,
            ParticipationRepository participationRepository,
            JourneyRepository journeyRepository,
            JourneyMemberRepository journeyMemberRepository,
            ChatRoomRepository chatRoomRepository,
            ChatRoomMemberRepository chatRoomMemberRepository,
            TimeProvider timeProvider,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.destinationRepository = destinationRepository;
        this.postRepository = postRepository;
        this.participationRepository = participationRepository;
        this.journeyRepository = journeyRepository;
        this.journeyMemberRepository = journeyMemberRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomMemberRepository = chatRoomMemberRepository;
        this.timeProvider = timeProvider;
        this.transactionManager = transactionManager;
    }

    ApplicationFixture application(String scenario) {
        return committed(() -> {
            String key = uniqueKey(scenario);
            User host = userRepository.save(user(key + "-host"));
            User applicant = userRepository.save(user(key + "-applicant"));
            Destination destination = destinationRepository.save(destination(key));
            Post post = postRepository.save(post(host, destination, 3, key));
            return new ApplicationFixture(host.getId(), applicant.getId(), post.getId());
        });
    }

    ApprovalFixture approval(String scenario) {
        return approval(scenario, 2);
    }

    ApprovalFixture approval(String scenario, int capacity) {
        return committed(() -> {
            String key = uniqueKey(scenario);
            User host = userRepository.save(user(key + "-host"));
            User firstApplicant = userRepository.save(user(key + "-first"));
            User secondApplicant = userRepository.save(user(key + "-second"));
            Destination destination = destinationRepository.save(destination(key));
            Post post = postRepository.save(post(host, destination, capacity, key));
            Journey journey = journeyRepository.save(Journey.create(post));
            journeyMemberRepository.save(JourneyMember.createHost(journey, host, timeProvider.now()));
            ChatRoom room = chatRoomRepository.save(ChatRoom.createGroupChat(journey));
            chatRoomMemberRepository.save(ChatRoomMember.create(room, host, ChatMemberRole.HOST));
            Participation first = participationRepository.save(
                    Participation.createParticipation(post, firstApplicant, "첫 신청")
            );
            Participation second = participationRepository.save(
                    Participation.createParticipation(post, secondApplicant, "두 번째 신청")
            );
            return new ApprovalFixture(
                    host.getId(), firstApplicant.getId(), secondApplicant.getId(), post.getId(), journey.getId(),
                    room.getId(), first.getId(), second.getId()
            );
        });
    }

    private <T> T committed(Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> work.get());
    }

    private User user(String key) {
        return User.builder()
                .email(key + "@example.com")
                .name(key)
                .oauthId(key)
                .oauthType(OauthType.KAKAO)
                .build();
    }

    private Destination destination(String key) {
        return Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("서울-" + key)
                .image("https://example.com/" + key + ".jpg")
                .build();
    }

    private Post post(User host, Destination destination, int capacity, String key) {
        var today = timeProvider.today();
        return Post.createPost(
                host, destination, "동시성 검증 " + key,
                "MySQL 동시성 통합 테스트를 위한 충분한 길이의 게시글 본문입니다.",
                today.plusDays(10), today.plusDays(12), capacity, today.plusDays(5),
                Gender.U, true, null, null, null, "[]", CompanionType.FULL
        );
    }

    private String uniqueKey(String scenario) {
        return scenario + "-" + SEQUENCE.incrementAndGet();
    }

    record ApplicationFixture(Long hostId, Long applicantId, Long postId) {
    }

    record ApprovalFixture(
            Long hostId,
            Long firstApplicantId,
            Long secondApplicantId,
            Long postId,
            Long journeyId,
            Long roomId,
            Long firstParticipationId,
            Long secondParticipationId
    ) {
    }
}
