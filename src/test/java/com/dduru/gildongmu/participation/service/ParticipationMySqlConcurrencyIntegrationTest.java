package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.chat.service.GroupChatRoomService;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyMember;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.domain.enums.ParticipationStatus;
import com.dduru.gildongmu.participation.dto.request.ParticipationRequest;
import com.dduru.gildongmu.participation.exception.DuplicateParticipationException;
import com.dduru.gildongmu.participation.exception.InvalidParticipationStatusException;
import com.dduru.gildongmu.participation.exception.RecruitmentClosedException;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.support.concurrency.ConcurrentRequestResult;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@DisplayName("참여 신청 MySQL 동시성 통합 테스트")
class ParticipationMySqlConcurrencyIntegrationTest extends MySqlIntegrationTestSupport {

    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    @Autowired private ParticipationApplicantService applicantService;
    @Autowired private ParticipationCommandService commandService;
    @Autowired private UserRepository userRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ParticipationRepository participationRepository;
    @Autowired private JourneyRepository journeyRepository;
    @Autowired private JourneyMemberRepository journeyMemberRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private GroupChatRoomService groupChatRoomService;

    @Test
    @DisplayName("같은 사용자와 게시글의 동시 신청은 한 건만 커밋한다")
    void concurrentDuplicateApplicationsCommitOnlyOnce() {
        ApplicationFixture fixture = createApplicationFixture("duplicate-application");
        ParticipationRequest request = new ParticipationRequest("MySQL 동시 신청 검증");

        var results = ConcurrentRequestRunner.run(
                List.of(
                        () -> applicantService.participate(fixture.applicantId(), fixture.postId(), request),
                        () -> applicantService.participate(fixture.applicantId(), fixture.postId(), request)
                ),
                failure -> hasCause(failure, DuplicateParticipationException.class)
        );

        assertThat(results).extracting("status")
                .containsExactlyInAnyOrder(
                        ConcurrentRequestStatus.SUCCESS,
                        ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT
                );
        assertNoUnexpectedDatabaseFailure(results);

        queryCommitted(() -> {
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND user_id = ?",
                    fixture.postId(), fixture.applicantId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM notifications n
                    JOIN participations p ON p.id = n.resource_id
                    WHERE n.type = 'MATCH_APPLIED' AND p.post_id = ? AND p.user_id = ?
                    """, fixture.postId(), fixture.applicantId())).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("마지막 한 자리의 동시 승인은 정원과 후속 저장을 초과하지 않는다")
    void concurrentApprovalsForLastSeatPreserveCapacityAndDependentRows() {
        ApprovalFixture fixture = createApprovalFixture("last-seat");

        var results = ConcurrentRequestRunner.run(
                List.of(
                        () -> commandService.approveParticipation(fixture.hostId(), fixture.firstParticipationId()),
                        () -> commandService.approveParticipation(fixture.hostId(), fixture.secondParticipationId())
                ),
                failure -> hasCause(failure, RecruitmentClosedException.class)
        );

        assertThat(results).extracting("status")
                .containsExactlyInAnyOrder(
                        ConcurrentRequestStatus.SUCCESS,
                        ConcurrentRequestStatus.EXPECTED_BUSINESS_CONFLICT
                );
        assertNoUnexpectedDatabaseFailure(results);

        queryCommitted(() -> {
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", fixture.postId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND status = 'APPROVED'",
                    fixture.postId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND status = 'ACTIVE'",
                    fixture.journeyId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND role = 'MEMBER'",
                    fixture.journeyId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND role = 'GUEST'",
                    fixture.roomId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    fixture.journeyId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM (
                        SELECT journey_id, user_id FROM journey_members
                        WHERE journey_id = ? GROUP BY journey_id, user_id HAVING COUNT(*) > 1
                    ) duplicates
                    """, fixture.journeyId())).isZero();
            assertThat(count("""
                    SELECT COUNT(*) FROM (
                        SELECT room_id, user_id FROM chat_room_members
                        WHERE room_id = ? GROUP BY room_id, user_id HAVING COUNT(*) > 1
                    ) duplicates
                    """, fixture.roomId())).isZero();
            return null;
        });
    }

    @Test
    @DisplayName("성공한 신청과 승인을 재호출해도 후속 행은 한 번만 생성한다")
    void repeatedRequestsDoNotDuplicateDependentRows() {
        ApplicationFixture application = createApplicationFixture("repeat-application");
        applicantService.participate(application.applicantId(), application.postId(), new ParticipationRequest("첫 신청"));

        assertThatThrownBy(() -> applicantService.participate(
                application.applicantId(), application.postId(), new ParticipationRequest("재신청")
        )).isInstanceOf(DuplicateParticipationException.class);

        ApprovalFixture approval = createApprovalFixture("repeat-approval", 3);
        commandService.approveParticipation(approval.hostId(), approval.firstParticipationId());

        assertThatThrownBy(() -> commandService.approveParticipation(
                approval.hostId(), approval.firstParticipationId()
        )).isInstanceOf(InvalidParticipationStatusException.class);

        queryCommitted(() -> {
            assertThat(count("SELECT COUNT(*) FROM participations WHERE post_id = ? AND user_id = ?",
                    application.postId(), application.applicantId())).isEqualTo(1);
            assertThat(count("""
                    SELECT COUNT(*) FROM notifications n
                    JOIN participations p ON p.id = n.resource_id
                    WHERE n.type = 'MATCH_APPLIED' AND p.post_id = ? AND p.user_id = ?
                    """, application.postId(), application.applicantId())).isEqualTo(1);
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", approval.postId())).isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND user_id = ?",
                    approval.journeyId(), approval.firstApplicantId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND user_id = ?",
                    approval.roomId(), approval.firstApplicantId())).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    approval.journeyId())).isEqualTo(1);
            return null;
        });
    }

    @Test
    @DisplayName("승인 후속 저장 실패는 모집 인원과 모든 후속 행을 롤백한다")
    void approvalDependentSaveFailureRollsBackWholeTransaction() {
        ApprovalFixture fixture = createApprovalFixture("approval-rollback");
        doThrow(new IllegalStateException("후속 그룹방 저장 실패 재현"))
                .when(groupChatRoomService)
                .inviteMemberOrGetRoom(anyLong(), anyLong(), anyLong());

        assertThatThrownBy(() -> commandService.approveParticipation(
                fixture.hostId(), fixture.firstParticipationId()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("후속 그룹방 저장 실패");

        queryCommitted(() -> {
            assertThat(integer("SELECT recruit_count FROM posts WHERE id = ?", fixture.postId())).isEqualTo(1);
            assertThat(string("SELECT status FROM participations WHERE id = ?", fixture.firstParticipationId()))
                    .isEqualTo(ParticipationStatus.PENDING.name());
            assertThat(count("SELECT COUNT(*) FROM journey_members WHERE journey_id = ? AND user_id = ?",
                    fixture.journeyId(), fixture.firstApplicantId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM chat_room_members WHERE room_id = ? AND user_id = ?",
                    fixture.roomId(), fixture.firstApplicantId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM notifications WHERE type = 'MATCH_APPROVED' AND resource_id = ?",
                    fixture.journeyId())).isZero();
            return null;
        });
    }

    private ApplicationFixture createApplicationFixture(String scenario) {
        return committed(() -> {
            String key = uniqueKey(scenario);
            User host = userRepository.save(user(key + "-host"));
            User applicant = userRepository.save(user(key + "-applicant"));
            Destination destination = destinationRepository.save(destination(key));
            Post post = postRepository.save(post(host, destination, 3, key));
            return new ApplicationFixture(host.getId(), applicant.getId(), post.getId());
        });
    }

    private ApprovalFixture createApprovalFixture(String scenario) {
        return createApprovalFixture(scenario, 2);
    }

    private ApprovalFixture createApprovalFixture(String scenario, int capacity) {
        return committed(() -> {
            String key = uniqueKey(scenario);
            User host = userRepository.save(user(key + "-host"));
            User firstApplicant = userRepository.save(user(key + "-first"));
            User secondApplicant = userRepository.save(user(key + "-second"));
            Destination destination = destinationRepository.save(destination(key));
            Post post = postRepository.save(post(host, destination, capacity, key));
            Journey journey = journeyRepository.save(Journey.create(post));
            journeyMemberRepository.save(JourneyMember.createHost(journey, host, LocalDateTime.now()));
            ChatRoom room = chatRoomRepository.save(ChatRoom.createGroupChat(journey));
            chatRoomMemberRepository.save(ChatRoomMember.create(room, host, ChatMemberRole.HOST));
            Participation first = participationRepository.save(Participation.createParticipation(post, firstApplicant, "첫 신청"));
            Participation second = participationRepository.save(Participation.createParticipation(post, secondApplicant, "두 번째 신청"));
            return new ApprovalFixture(
                    host.getId(), firstApplicant.getId(), secondApplicant.getId(), post.getId(), journey.getId(), room.getId(),
                    first.getId(), second.getId()
            );
        });
    }

    private static User user(String key) {
        return User.builder()
                .email(key + "@example.com")
                .name(key)
                .oauthId(key)
                .oauthType(OauthType.KAKAO)
                .build();
    }

    private static Destination destination(String key) {
        return Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("서울-" + key)
                .image("https://example.com/" + key + ".jpg")
                .build();
    }

    private static Post post(User host, Destination destination, int capacity, String key) {
        LocalDate today = LocalDate.now();
        return Post.createPost(
                host, destination, "동시성 검증 " + key, "MySQL 동시성 통합 테스트를 위한 충분한 길이의 게시글 본문입니다.",
                today.plusDays(10), today.plusDays(12), capacity, today.plusDays(5),
                Gender.U, true, null, null, null, "[]", CompanionType.FULL
        );
    }

    private static String uniqueKey(String scenario) {
        return scenario + "-" + FIXTURE_SEQUENCE.incrementAndGet();
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return value == null ? 0 : value;
    }

    private int integer(String sql, Object... arguments) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return value == null ? 0 : value;
    }

    private String string(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, String.class, arguments);
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

    private static void assertNoUnexpectedDatabaseFailure(List<ConcurrentRequestResult> results) {
        assertThat(results).allMatch(result -> result.status() != ConcurrentRequestStatus.DEADLOCK);
        assertThat(results).allMatch(result -> result.status() != ConcurrentRequestStatus.LOCK_TIMEOUT);
        assertThat(results).allMatch(result -> result.status() != ConcurrentRequestStatus.UNEXPECTED_FAILURE);
    }

    private record ApplicationFixture(Long hostId, Long applicantId, Long postId) {
    }

    private record ApprovalFixture(
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
