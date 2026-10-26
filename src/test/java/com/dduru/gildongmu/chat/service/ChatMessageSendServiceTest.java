package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.dto.ws.ChatMessageSendRequest;
import com.dduru.gildongmu.chat.event.ChatMessageCreatedEvent;
import com.dduru.gildongmu.chat.constants.ChatDestinationPaths;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.dto.ws.ChatMessageBroadcastPayload;
import com.dduru.gildongmu.chat.exception.ChatPollMessageSendAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatRoomClosedException;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.chat.system.ChatSystemMessageFactory;
import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageSendService 테스트")
class ChatMessageSendServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileImageResolver profileImageResolver;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private ChatSystemMessageFactory chatSystemMessageFactory;

    @Mock
    private S3ImageUrlValidator s3ImageUrlValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatMessageSendService service;

    @BeforeEach
    void setUp() {
        service = new ChatMessageSendService(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatMessageRepository,
                userRepository,
                profileImageResolver,
                simpMessagingTemplate,
                chatSystemMessageFactory,
                s3ImageUrlValidator,
                eventPublisher
        );
    }

    @Test
    @DisplayName("사용자 메시지 저장에 성공하면 채팅방 목록 갱신 이벤트를 발행한다")
    void sendUserMessagePublishesChatMessageCreatedEvent() {
        Long roomId = 1L;
        Long senderUserId = 10L;
        User sender = createUser(senderUserId, "sender");
        Post post = createPost(100L, sender);
        ChatRoom room = ChatRoom.forPrivateChat(post);
        ReflectionTestUtils.setField(room, "id", roomId);
        ChatRoomMember senderMember = ChatRoomMember.create(room, sender, ChatMemberRole.HOST);

        when(chatRoomRepository.getByIdOrThrow(roomId)).thenReturn(room);
        when(chatRoomMemberRepository.findByRoomIdAndUserIdWithLock(roomId, senderUserId))
                .thenReturn(Optional.of(senderMember));
        when(userRepository.getWithProfileByIdOrThrow(senderUserId)).thenReturn(sender);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 123L);
            return message;
        });

        service.sendUserMessage(
                senderUserId,
                roomId,
                new ChatMessageSendRequest(ChatMessageType.TEXT, "안녕하세요")
        );

        verify(eventPublisher).publishEvent(new ChatMessageCreatedEvent(roomId, 123L));
    }

    private static User createUser(Long userId, String name) {
        User user = User.builder()
                .email(name + "@example.com")
                .name(name)
                .oauthId("oauth-" + userId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Profile profile = new Profile(user);
        ReflectionTestUtils.setField(user, "profile", profile);
        return user;
    }

    @Test
    void publishesPollCardAndAdvancesSenderReadCursor() {
        User sender = createUser(10L, "sender");
        Journey journey = Journey.create(createPost(100L, sender));
        ReflectionTestUtils.setField(journey, "id", 1L);
        ChatRoom room = ChatRoom.createGroupChat(journey);
        ReflectionTestUtils.setField(room, "id", 20L);
        ChatRoomMember member = ChatRoomMember.create(room, sender, ChatMemberRole.HOST);
        LocalDateTime now = LocalDateTime.now();
        JourneyPoll poll = JourneyPoll.create(journey, sender, "Dinner", true, true, true, now.plusDays(1), now);
        ReflectionTestUtils.setField(poll, "id", 5L);
        JourneyPollSummaryResponse summary = JourneyPollSummaryResponse.from(poll, now, 0, List.of(), null);
        when(chatRoomRepository.findByJourneyIdAndRoomType(1L, ChatRoomType.GROUP)).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByRoomIdAndUserIdWithLock(20L, 10L)).thenReturn(Optional.of(member));
        when(chatMessageRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 123L);
            return message;
        });

        service.publishPoll(poll, summary);

        assertThat(member.getLastReadMessage().getPoll()).isSameAs(poll);
        verify(eventPublisher).publishEvent(new ChatMessageCreatedEvent(20L, 123L));
        ArgumentCaptor<ChatMessageBroadcastPayload> payload = ArgumentCaptor.forClass(ChatMessageBroadcastPayload.class);
        verify(simpMessagingTemplate).convertAndSend(eq(ChatDestinationPaths.topicRoom(20L)), payload.capture());
        assertThat(payload.getValue().messageType()).isEqualTo(ChatMessageType.POLL);
        assertThat(payload.getValue().poll().selectedOptionIds()).isNull();
        assertThat(payload.getValue().userMessage()).isNull();
    }

    @Test
    void rejectsClientForgedPollMessages() {
        User sender = createUser(10L, "sender");
        ChatRoom room = ChatRoom.forPrivateChat(createPost(100L, sender));
        when(chatRoomRepository.getByIdOrThrow(20L)).thenReturn(room);
        when(chatRoomMemberRepository.findByRoomIdAndUserIdWithLock(20L, 10L))
                .thenReturn(Optional.of(ChatRoomMember.create(room, sender, ChatMemberRole.HOST)));
        when(userRepository.getWithProfileByIdOrThrow(10L)).thenReturn(sender);
        assertThatThrownBy(() -> service.sendUserMessage(10L, 20L, new ChatMessageSendRequest(ChatMessageType.POLL, "5")))
                .isInstanceOf(ChatPollMessageSendAccessDeniedException.class);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void closedRoomCannotReceivePollCard() {
        User sender = createUser(10L, "sender");
        Journey journey = Journey.create(createPost(100L, sender));
        ReflectionTestUtils.setField(journey, "id", 1L);
        ChatRoom room = ChatRoom.createGroupChat(journey);
        ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
        when(chatRoomRepository.findByJourneyIdAndRoomType(1L, ChatRoomType.GROUP)).thenReturn(Optional.of(room));
        JourneyPoll poll = JourneyPoll.create(journey, sender, "Dinner", false, false, false,
                LocalDateTime.now().plusDays(1), LocalDateTime.now());
        assertThatThrownBy(() -> service.publishPoll(poll, null)).isInstanceOf(ChatRoomClosedException.class);
        verify(chatMessageRepository, never()).saveAndFlush(any());
    }

    private static Post createPost(Long postId, User owner) {
        Post post = Post.createPost(
                owner,
                null,
                "채팅 테스트 게시글",
                "채팅 테스트 본문은 충분히 긴 내용입니다.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                4,
                LocalDate.now().plusDays(1),
                Gender.U,
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
}
