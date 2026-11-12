package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.block.service.UserBlockService;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.query.ChatRoomIdByPostIdQueryResult;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.JourneyMemberStatusQueryResult;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.domain.enums.ParticipationStatus;
import com.dduru.gildongmu.participation.dto.response.MyParticipationResponse;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.dto.response.MyParticipationStatus;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationApplicantService 테스트")
class ParticipationApplicantServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 11, 12, 0);

    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ProfileImageResolver profileImageResolver;
    @Mock
    private JourneyMemberRepository journeyMemberRepository;
    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private ParticipationApplicantService participationApplicantService;

    @Nested
    @DisplayName("내 신청 목록 조회")
    class RetrieveMyApplications {

        @Test
        @DisplayName("승인 신청의 여정 상태와 그룹 채팅방을 한 번에 조회한다")
        void retrievesApprovedJourneyStatusesAndGroupRoomsInBulk() {
            Long userId = 20L;
            Long activePostId = 1L;
            Long removedPostId = 2L;
            Long pendingPostId = 3L;
            Long groupRoomId = 100L;
            Participation activeParticipation = createParticipation(11L, activePostId, userId, ParticipationStatus.APPROVED);
            Participation removedParticipation = createParticipation(12L, removedPostId, userId, ParticipationStatus.APPROVED);
            Participation pendingParticipation = createParticipation(13L, pendingPostId, userId, ParticipationStatus.PENDING);

            when(participationRepository.findMyApplicationsForVisiblePosts(userId))
                    .thenReturn(List.of(activeParticipation, removedParticipation, pendingParticipation));
            when(journeyMemberRepository.findStatusesByPostIdsAndUserId(Set.of(activePostId, removedPostId), userId))
                    .thenReturn(List.of(
                            new JourneyMemberStatusQueryResult(activePostId, JourneyMemberStatus.ACTIVE),
                            new JourneyMemberStatusQueryResult(removedPostId, JourneyMemberStatus.REMOVED)
                    ));
            when(chatRoomRepository.findRoomIdsByJourneyPostIdsAndRoomType(Set.of(activePostId), ChatRoomType.GROUP))
                    .thenReturn(List.of(new ChatRoomIdByPostIdQueryResult(activePostId, groupRoomId)));

            List<MyParticipationResponse> responses = participationApplicantService.retrieveMyApplications(userId);

            assertThat(responses).extracting(MyParticipationResponse::status)
                    .containsExactly(
                            MyParticipationStatus.APPROVED,
                            MyParticipationStatus.REMOVED_BY_HOST,
                            MyParticipationStatus.PENDING
                    );
            assertThat(responses).extracting(MyParticipationResponse::groupRoomId)
                    .containsExactly(groupRoomId, null, null);
            verify(journeyMemberRepository).findStatusesByPostIdsAndUserId(Set.of(activePostId, removedPostId), userId);
            verify(journeyMemberRepository, never()).findStatusByPostIdAndUserId(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("내 참여 상태 조회")
    class GetMyParticipationStatus {

        @Test
        @DisplayName("승인 이력이 있어도 여정 멤버가 REMOVED이면 REMOVED_BY_HOST를 반환한다")
        void approvedParticipationWithRemovedJourneyMemberReturnsRemovedByHost() {
            Long postId = 1L;
            Long userId = 20L;
            Participation participation = createApprovedParticipation(postId, userId);

            when(participationRepository.findByPostIdAndUserId(postId, userId))
                    .thenReturn(Optional.of(participation));
            when(journeyMemberRepository.findStatusByPostIdAndUserId(postId, userId))
                    .thenReturn(Optional.of(JourneyMemberStatus.REMOVED));

            MyParticipationStatus status = participationApplicantService.getMyParticipationStatus(postId, userId, false);

            assertThat(status).isEqualTo(MyParticipationStatus.REMOVED_BY_HOST);
        }
    }

    private Participation createApprovedParticipation(Long postId, Long userId) {
        return createParticipation(null, postId, userId, ParticipationStatus.APPROVED);
    }

    private Participation createParticipation(Long participationId, Long postId, Long userId, ParticipationStatus status) {
        User author = createUser(10L, "author");
        User participant = createUser(userId, "participant");
        Post post = Post.createPost(
                author,
                null,
                "참여 상태 테스트 게시글",
                "참여 상태 테스트 본문은 충분히 긴 내용입니다.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                3,
                LocalDate.now().plusDays(1),
                null,
                true,
                null,
                null,
                null,
                "[]",
                null
        );
        ReflectionTestUtils.setField(post, "id", postId);

        Participation participation = Participation.createParticipation(post, participant, "hello");
        ReflectionTestUtils.setField(participation, "id", participationId);
        applyStatus(participation, status);
        return participation;
    }

    private void applyStatus(Participation participation, ParticipationStatus status) {
        switch (status) {
            case PENDING -> {
            }
            case CONTACTING -> participation.contact(NOW);
            case APPROVED -> participation.approve(NOW);
            case REJECTED -> participation.reject(NOW);
        }
    }

    private User createUser(Long userId, String name) {
        User user = User.builder()
                .email(name + "@example.com")
                .name(name)
                .oauthId("oauth-" + userId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
