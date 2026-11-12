package com.dduru.gildongmu.participation.service;

import com.dduru.gildongmu.block.service.UserBlockService;
import com.dduru.gildongmu.chat.dto.response.GroupChatInviteMemberResponse;
import com.dduru.gildongmu.chat.dto.response.PrivateChatRoomCreateResponse;
import com.dduru.gildongmu.chat.service.GroupChatRoomService;
import com.dduru.gildongmu.chat.service.PrivateChatRoomService;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.participation.domain.Participation;
import com.dduru.gildongmu.participation.domain.enums.ParticipationStatus;
import com.dduru.gildongmu.participation.dto.response.ParticipationApproveResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationContactResponse;
import com.dduru.gildongmu.participation.repository.ParticipationRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationCommandService 테스트")
class ParticipationCommandServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 11, 12, 0);

    @Mock
    private PrivateChatRoomService privateChatRoomService;

    @Mock
    private GroupChatRoomService groupChatRoomService;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ProfileImageResolver profileImageResolver;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JourneyRepository journeyRepository;

    @Mock
    private JourneyMemberRepository journeyMemberRepository;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserBlockService userBlockService;

    @InjectMocks
    private ParticipationCommandService participationCommandService;

    @Nested
    @DisplayName("연락 시작")
    class ContactParticipation {

        @Test
        @DisplayName("엔티티 상태 전이 메서드를 사용해 CONTACTING 상태를 반환한다")
        void returnsContactingStatus() {
            Long participationId = 1L;
            Long ownerId = 10L;
            Long participantId = 20L;
            Long roomId = 30L;

            User owner = createUser(ownerId, "owner");
            Post post = createPost(100L, owner);
            Participation participation = createParticipation(participationId, post, participantId);

            when(participationRepository.getByIdWithLockOrThrow(participationId)).thenReturn(participation);
            when(postRepository.getActiveByIdWithLockOrThrow(post.getId())).thenReturn(post);
            when(timeProvider.now()).thenReturn(NOW);
            when(privateChatRoomService.createOrGetRoomWithLockedPost(ownerId, post, participantId))
                    .thenReturn(new PrivateChatRoomCreateResponse(roomId, true));

            ParticipationContactResponse response = participationCommandService.contactParticipation(ownerId, participationId);

            assertThat(response.privateRoomId()).isEqualTo(roomId);
            assertThat(response.status()).isEqualTo(ParticipationStatus.CONTACTING);
            assertThat(participation.isContacting()).isTrue();
            verify(privateChatRoomService).createOrGetRoomWithLockedPost(ownerId, post, participantId);
        }
    }

    @Nested
    @DisplayName("참여 승인")
    class ApproveParticipation {

        @Test
        @DisplayName("Post.approveParticipation을 통해 APPROVED 상태를 반환한다")
        void returnsApprovedStatus() {
            Long participationId = 1L;
            Long ownerId = 10L;
            Long participantId = 20L;
            Long roomId = 40L;

            User owner = createUser(ownerId, "owner");
            Post post = createPost(100L, owner);
            Journey journey = createJourney(500L, post);
            Participation participation = createParticipation(participationId, post, participantId);
            participation.contact(NOW);

            Profile ownerProfile = mock(Profile.class);
            when(ownerProfile.getNickname()).thenReturn("호스트닉네임");

            when(participationRepository.getByIdWithLockOrThrow(participationId)).thenReturn(participation);
            when(postRepository.getActiveByIdWithLockOrThrow(post.getId())).thenReturn(post);
            when(timeProvider.now()).thenReturn(NOW);
            when(journeyRepository.getByPostIdOrThrow(post.getId())).thenReturn(journey);
            when(profileRepository.findByUser_Id(ownerId)).thenReturn(Optional.of(ownerProfile));
            when(groupChatRoomService.inviteMemberOrGetRoom(ownerId, journey.getId(), participantId))
                    .thenReturn(new GroupChatInviteMemberResponse(roomId, true));

            ParticipationApproveResponse response = participationCommandService.approveParticipation(ownerId, participationId);

            assertThat(response.groupRoomId()).isEqualTo(roomId);
            assertThat(response.status()).isEqualTo(ParticipationStatus.APPROVED);
            assertThat(participation.isApproved()).isTrue();
            assertThat(post.getRecruitCount()).isEqualTo(2);
            verify(journeyMemberRepository).save(any());
            verify(groupChatRoomService).inviteMemberOrGetRoom(ownerId, journey.getId(), participantId);
        }
    }

    private Participation createParticipation(Long participationId, Post post, Long participantId) {
        Participation participation = Participation.createParticipation(post, createUser(participantId, "participant"), "hello");
        ReflectionTestUtils.setField(participation, "id", participationId);
        return participation;
    }

    private Post createPost(Long postId, User owner) {
        Post post = Post.createPost(
                owner,
                null,
                "참여 승인 테스트 게시글",
                "참여 승인 테스트 본문은 충분히 긴 내용입니다.",
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
        return post;
    }

    private Journey createJourney(Long journeyId, Post post) {
        Journey journey = Journey.create(post);
        ReflectionTestUtils.setField(journey, "id", journeyId);
        return journey;
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
