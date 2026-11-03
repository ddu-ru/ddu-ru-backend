package com.dduru.gildongmu.chat.event;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.service.ChatOnlineStatusService;
import com.dduru.gildongmu.chat.service.ChatPushNotificationService;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessagePushEventListener 테스트")
class ChatMessagePushEventListenerTest {

    private static final Long ROOM_ID = 10L;
    private static final Long SENDER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatOnlineStatusService chatOnlineStatusService;
    @Mock private ChatPushNotificationService chatPushNotificationService;
    @Mock private UserRepository userRepository;

    private ChatMessagePushEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChatMessagePushEventListener(
                chatMessageRepository, chatRoomMemberRepository,
                chatOnlineStatusService, chatPushNotificationService, userRepository
        );
    }

    @Nested
    @DisplayName("필터 — 조기 종료 케이스")
    class EarlyReturnCases {

        @Test
        @DisplayName("메시지를 찾을 수 없으면 푸시를 발송하지 않는다")
        void skipsWhenMessageNotFound() {
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.empty());

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService, never()).sendPush(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("SYSTEM 메시지는 푸시를 발송하지 않는다")
        void skipsSystemMessage() {
            ChatMessage message = createMessage(ChatMessageType.SYSTEM, "홍길동 님이 입장하였습니다.");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatRoomMemberRepository, never()).findNotificationEnabledUserIdsByRoomId(any());
        }

        @Test
        @DisplayName("발신자 외 수신자가 없으면 푸시를 발송하지 않는다")
        void skipsWhenNoRecipients() {
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "안녕");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID)).thenReturn(List.of(SENDER_ID));

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatOnlineStatusService, never()).getOnlineUserIds(any());
        }

        @Test
        @DisplayName("수신자 전원이 채팅방에 접속 중이면 푸시를 발송하지 않는다")
        void skipsWhenAllRecipientsOnline() {
            Long recipientId = 2L;
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "안녕");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID)).thenReturn(List.of(SENDER_ID, recipientId));
            when(chatOnlineStatusService.getOnlineUserIds(ROOM_ID)).thenReturn(Set.of(recipientId));

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService, never()).canSendPush(any());
        }

        @Test
        @DisplayName("30초 쿨다운 중이면 푸시를 발송하지 않는다")
        void skipsWhenCooldownActive() {
            Long recipientId = 2L;
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "안녕");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID)).thenReturn(List.of(SENDER_ID, recipientId));
            when(chatOnlineStatusService.getOnlineUserIds(ROOM_ID)).thenReturn(Set.of());
            when(userRepository.findEnabledUserIds(List.of(recipientId))).thenReturn(List.of(recipientId));
            when(chatPushNotificationService.canSendPush(ROOM_ID)).thenReturn(false);

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService, never()).sendPush(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("알림 수신 off 유저만 남으면 FCM을 발송하지 않는다")
        void skipsWhenAllNotificationDisabled() {
            Long recipientId = 2L;
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "안녕");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID)).thenReturn(List.of(SENDER_ID, recipientId));
            when(chatOnlineStatusService.getOnlineUserIds(ROOM_ID)).thenReturn(Set.of());
            when(userRepository.findEnabledUserIds(List.of(recipientId))).thenReturn(List.of());

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService, never()).sendPush(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("정상 발송")
    class NormalSend {

        @Test
        @DisplayName("GROUP 방은 여정 제목을 title로 FCM을 발송한다")
        void sendsGroupPushWithJourneyTitle() {
            Long recipientId = 2L;
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "오늘 저녁 7시 어때?");
            setupNormalSendMocks(message, recipientId);

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService).sendPush(
                    eq(List.of(recipientId)), eq("도쿄 여행"), eq("홍길동"),
                    eq("오늘 저녁 7시 어때?"), eq(ChatMessageType.TEXT), eq(ChatRoomType.GROUP), eq(ROOM_ID)
            );
        }

        @Test
        @DisplayName("PRIVATE 방은 발신자 닉네임을 title로 FCM을 발송한다")
        void sendsPrivatePushWithSenderNickname() {
            Long recipientId = 2L;
            ChatMessage message = createPrivateMessage(ChatMessageType.TEXT, "안녕하세요");
            setupNormalSendMocks(message, recipientId);

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService).sendPush(
                    eq(List.of(recipientId)), eq("홍길동"), eq("홍길동"),
                    eq("안녕하세요"), eq(ChatMessageType.TEXT), eq(ChatRoomType.PRIVATE), eq(ROOM_ID)
            );
        }

        @Test
        @DisplayName("이미지 메시지도 정상 발송한다")
        void sendsImagePush() {
            Long recipientId = 2L;
            ChatMessage message = createGroupMessage(ChatMessageType.IMAGE, "https://s3.example.com/img.jpg");
            setupNormalSendMocks(message, recipientId);

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService).sendPush(
                    any(), any(), any(), any(), eq(ChatMessageType.IMAGE), eq(ChatRoomType.GROUP), eq(ROOM_ID)
            );
        }

        @Test
        @DisplayName("접속 중인 유저를 제외한 미접속 수신자에게만 발송한다")
        void sendsOnlyToOfflineRecipients() {
            Long onlineId = 2L;
            Long offlineId = 3L;
            ChatMessage message = createGroupMessage(ChatMessageType.TEXT, "ㅎㅇ");
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID))
                    .thenReturn(List.of(SENDER_ID, onlineId, offlineId));
            when(chatOnlineStatusService.getOnlineUserIds(ROOM_ID)).thenReturn(Set.of(onlineId));
            when(chatPushNotificationService.canSendPush(ROOM_ID)).thenReturn(true);
            when(userRepository.findEnabledUserIds(List.of(offlineId))).thenReturn(List.of(offlineId));

            listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

            verify(chatPushNotificationService).sendPush(
                    eq(List.of(offlineId)), any(), any(), any(), any(), any(), any()
            );
        }

        private void setupNormalSendMocks(ChatMessage message, Long recipientId) {
            when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID))
                    .thenReturn(List.of(SENDER_ID, recipientId));
            when(chatOnlineStatusService.getOnlineUserIds(ROOM_ID)).thenReturn(Set.of());
            when(chatPushNotificationService.canSendPush(ROOM_ID)).thenReturn(true);
            when(userRepository.findEnabledUserIds(List.of(recipientId))).thenReturn(List.of(recipientId));
        }
    }

    @ParameterizedTest
    @EnumSource(value = ChatMessageType.class, names = {"TEXT", "IMAGE", "POLL"})
    @DisplayName("방별 알림 ON 수신자가 없으면 모든 메시지 타입에서 쿨다운도 사용하지 않는다")
    void skipsMutedRoomWithoutConsumingCooldown(ChatMessageType type) {
        ChatMessage message = createGroupMessage(type, "메시지");
        when(chatMessageRepository.findByIdWithSenderAndRoom(MESSAGE_ID))
                .thenReturn(Optional.of(message));
        when(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(ROOM_ID)).thenReturn(List.of());

        listener.handle(new ChatMessageCreatedEvent(ROOM_ID, MESSAGE_ID));

        verifyNoInteractions(chatOnlineStatusService, userRepository, chatPushNotificationService);
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────────────────────

    private ChatMessage createGroupMessage(ChatMessageType type, String content) {
        User sender = createSender();
        ChatRoom room = mock(ChatRoom.class);
        Journey journey = mock(Journey.class);
        lenient().when(room.getRoomType()).thenReturn(ChatRoomType.GROUP);
        lenient().when(room.getJourney()).thenReturn(journey);
        lenient().when(journey.getTitle()).thenReturn("도쿄 여행");
        return buildMessage(room, sender, type, content);
    }

    private ChatMessage createPrivateMessage(ChatMessageType type, String content) {
        User sender = createSender();
        ChatRoom room = mock(ChatRoom.class);
        lenient().when(room.getRoomType()).thenReturn(ChatRoomType.PRIVATE);
        return buildMessage(room, sender, type, content);
    }

    private ChatMessage createMessage(ChatMessageType type, String content) {
        User sender = createSender();
        ChatRoom room = mock(ChatRoom.class);
        return buildMessage(room, sender, type, content);
    }

    private ChatMessage buildMessage(ChatRoom room, User sender, ChatMessageType type, String content) {
        ChatMessage message = ChatMessage.create(room, sender, type, content);
        ReflectionTestUtils.setField(message, "id", MESSAGE_ID);
        return message;
    }

    private User createSender() {
        User user = User.builder()
                .email("hong@example.com")
                .name("홍길동")
                .oauthId("oauth-1")
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", SENDER_ID);
        Profile profile = new Profile(user);
        ReflectionTestUtils.setField(profile, "nickname", "홍길동");
        ReflectionTestUtils.setField(user, "profile", profile);
        return user;
    }
}
