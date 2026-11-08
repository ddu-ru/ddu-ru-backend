package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.dto.request.ChatMessageRetrieveRequest;
import com.dduru.gildongmu.chat.dto.request.ChatContentListRequest;
import com.dduru.gildongmu.chat.dto.response.ChatImageListResponse;
import com.dduru.gildongmu.chat.dto.response.ChatPollListResponse;
import com.dduru.gildongmu.chat.dto.response.ChatMessagesResponse;
import com.dduru.gildongmu.chat.dto.ws.ChatSystemMessagePayload;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatMessageNotFoundException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.chat.system.ChatSystemMessageFactory;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.service.JourneyPollQueryService;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollOptionResponse;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageQueryService 테스트")
class ChatMessageQueryServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private JourneyMemberRepository journeyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatSystemMessageFactory chatSystemMessageFactory;

    @Mock
    private JourneyPollQueryService journeyPollQueryService;

    private ChatMessageQueryService chatMessageQueryService;

    @BeforeEach
    void setUp() {
        chatMessageQueryService = new ChatMessageQueryService(
                chatRoomRepository,
                chatRoomMemberRepository,
                chatMessageRepository,
                journeyMemberRepository,
                userRepository,
                chatSystemMessageFactory,
                journeyPollQueryService
        );
    }

    @Nested
    @DisplayName("메시지 조회")
    class RetrieveMessages {

        @Test
        void retrievesPollCardWithLatestCountsAndOwnSelections() {
            LocalDateTime joinedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(10L, "host", "hostNick");
            Journey journey = createJourney(30L, createPost(1L, host, "Poll trip"), "Poll trip");
            ChatRoom room = createGroupRoom(200L, journey, ChatRoomStatus.ACTIVE);
            ChatRoomMember member = createMember(room, host, ChatMemberRole.HOST, joinedAt);
            JourneyPoll poll = JourneyPoll.create(journey, host, "Dinner", true, true, false,
                    joinedAt.plusDays(1), joinedAt);
            ReflectionTestUtils.setField(poll, "id", 5L);
            ChatMessage message = ChatMessage.forPoll(room, poll);
            ReflectionTestUtils.setField(message, "id", 441L);
            ReflectionTestUtils.setField(message, "createdAt", joinedAt.plusMinutes(1));
            JourneyPollSummaryResponse summary = JourneyPollSummaryResponse.from(poll, joinedAt, 2,
                    List.of(new JourneyPollOptionResponse(11L, "A", 2)), List.of(11L));
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(200L, 10L)).thenReturn(Optional.of(member));
            when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(30L, 10L, JourneyMemberStatus.ACTIVE)).thenReturn(true);
            when(chatMessageRepository.findVisibleMessages(eq(200L), eq(null), eq(joinedAt), any(Pageable.class)))
                    .thenReturn(List.of(message));
            when(chatRoomMemberRepository.findByRoomIdWithLastReadMessage(200L)).thenReturn(List.of(member));
            when(journeyPollQueryService.buildSummaries(List.of(poll), 10L)).thenReturn(java.util.Map.of(5L, summary));

            ChatMessagesResponse response = chatMessageQueryService.retrieveMessages(10L, 200L, new ChatMessageRetrieveRequest(null, null));

            assertThat(response.messages()).hasSize(1);
            assertThat(response.messages().get(0).poll()).isEqualTo(summary);
            assertThat(response.messages().get(0).unreadCount()).isZero();
            assertThat(response.messages().get(0).content()).isNull();
            assertThat(response.messages().get(0).systemMessage()).isNull();
        }

        @Test
        @DisplayName("1:1 방 최초 조회는 최신 메시지를 오래된 순으로 반환하고 다음 커서를 제공한다")
        void retrievesPrivateMessagesInAscendingOrder() {
            Long roomId = 100L;
            Long currentUserId = 10L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(currentUserId, "host", "hostNick");
            User guest = createUser(20L, "guest", "guestNick");
            Post post = createPost(1L, host, "코끼리 아저씨");
            ChatRoom room = createPrivateRoom(roomId, post, ChatRoomStatus.ACTIVE);
            ChatRoomMember currentMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);
            ChatRoomMember guestMember = createMember(room, guest, ChatMemberRole.GUEST, memberCreatedAt);

            ChatMessage oldestReturned = createMessage(102L, room, host, ChatMessageType.IMAGE,
                    "https://cdn.example.com/chats/image.jpg", memberCreatedAt.plusMinutes(2));
            ChatMessage newestReturned = createMessage(103L, room, guest, ChatMessageType.TEXT,
                    "시간 다르면 다르게 뜨도록", memberCreatedAt.plusMinutes(3));
            ChatMessage lookAhead = createMessage(101L, room, guest, ChatMessageType.TEXT,
                    "이전 메시지", memberCreatedAt.plusMinutes(1));

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, currentUserId)).thenReturn(Optional.of(currentMember));
            when(chatRoomMemberRepository.findByRoomIdWithUserProfile(roomId))
                    .thenReturn(List.of(currentMember, guestMember));
            when(chatMessageRepository.findVisibleMessages(eq(roomId), eq(null), eq(memberCreatedAt), any(Pageable.class)))
                    .thenReturn(List.of(newestReturned, oldestReturned, lookAhead));
            when(chatRoomMemberRepository.findByRoomIdWithLastReadMessage(roomId))
                    .thenReturn(List.of(currentMember, guestMember));

            ChatMessagesResponse response = chatMessageQueryService.retrieveMessages(
                    currentUserId,
                    roomId,
                    new ChatMessageRetrieveRequest(null, 2)
            );

            assertThat(response.roomInfo().chatRoomId()).isEqualTo(roomId);
            assertThat(response.roomInfo().postTitle()).isEqualTo("코끼리 아저씨");
            assertThat(response.roomInfo().opponentNickname()).isEqualTo("guestNick");
            assertThat(response.roomInfo().isActive()).isTrue();
            assertThat(response.page().size()).isEqualTo(2);
            assertThat(response.page().hasNext()).isTrue();
            assertThat(response.page().nextCursor()).isEqualTo(102L);
            assertThat(response.messages()).extracting("messageId").containsExactly(102L, 103L);
            assertThat(response.messages().get(0).messageType()).isEqualTo(ChatMessageType.IMAGE);
            assertThat(response.messages().get(0).content()).isNull();
            assertThat(response.messages().get(0).images().get(0).imageUrl())
                    .isEqualTo("https://cdn.example.com/chats/image.jpg");
            assertThat(response.messages().get(0).sender().isHost()).isTrue();
            assertThat(response.messages().get(0).isMine()).isTrue();
            assertThat(response.messages().get(0).unreadCount()).isEqualTo(1);
            assertThat(response.messages().get(1).content()).isEqualTo("시간 다르면 다르게 뜨도록");
            assertThat(response.messages().get(1).unreadCount()).isEqualTo(1);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(chatMessageRepository).findVisibleMessages(eq(roomId), eq(null), eq(memberCreatedAt), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("그룹 방 시스템 메시지는 displayText를 서버에서 조립한다")
        void retrievesGroupSystemMessage() {
            Long roomId = 200L;
            Long hostUserId = 10L;
            Long inviteeUserId = 20L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(hostUserId, "host", "hostNick");
            User invitee = createUser(inviteeUserId, "guest", "guestNick");
            Post post = createPost(1L, host, "제주 애월 모집");
            Journey journey = createJourney(30L, post, "제주 애월 2박 3일");
            ChatRoom room = createGroupRoom(roomId, journey, ChatRoomStatus.CLOSED);
            ChatRoomMember currentMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);
            ChatMessage systemMessage = createMessage(441L, room, null, ChatMessageType.SYSTEM,
                    "{\"type\":\"USER_INVITED\"}", memberCreatedAt.plusMinutes(1));
            ChatSystemMessagePayload payload = ChatSystemMessagePayload.userInvited(inviteeUserId, hostUserId);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, hostUserId)).thenReturn(Optional.of(currentMember));
            when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(30L, hostUserId, JourneyMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(journeyMemberRepository.countByJourneyIdAndStatus(30L, JourneyMemberStatus.ACTIVE)).thenReturn(3);
            when(chatMessageRepository.findVisibleMessages(eq(roomId), eq(null), eq(memberCreatedAt), any(Pageable.class)))
                    .thenReturn(List.of(systemMessage));
            when(chatSystemMessageFactory.deserialize(systemMessage.getContent())).thenReturn(payload);
            when(userRepository.findAllWithProfileByIdIn(any(Collection.class))).thenReturn(List.of(host, invitee));

            ChatMessagesResponse response = chatMessageQueryService.retrieveMessages(
                    hostUserId,
                    roomId,
                    new ChatMessageRetrieveRequest(null, null)
            );

            assertThat(response.roomInfo().journeyTitle()).isEqualTo("제주 애월 2박 3일");
            assertThat(response.roomInfo().memberCount()).isEqualTo(3);
            assertThat(response.roomInfo().isActive()).isFalse();
            assertThat(response.page().hasNext()).isFalse();
            assertThat(response.page().nextCursor()).isNull();
            assertThat(response.messages()).hasSize(1);
            assertThat(response.messages().get(0).sender()).isNull();
            assertThat(response.messages().get(0).isMine()).isFalse();
            assertThat(response.messages().get(0).systemMessage().displayText())
                    .isEqualTo("guestNick 님이 그룹 채팅방에 참여했습니다.");
            assertThat(response.messages().get(0).systemMessage().actorUserId()).isEqualTo(hostUserId);
            assertThat(response.messages().get(0).systemMessage().inviteeUserId()).isEqualTo(inviteeUserId);
        }

        @Test
        @DisplayName("그룹 메시지 unreadCount는 sender, 읽은 멤버, 메시지 이후 참여자를 제외하고 계산한다")
        void calculatesGroupUnreadCount() {
            Long roomId = 200L;
            Long hostUserId = 10L;
            Long unreadUserId = 20L;
            Long readUserId = 30L;
            Long newcomerUserId = 40L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(hostUserId, "host", "hostNick");
            User unreadMemberUser = createUser(unreadUserId, "unread", "unreadNick");
            User readMemberUser = createUser(readUserId, "read", "readNick");
            User newcomer = createUser(newcomerUserId, "newcomer", "newcomerNick");
            Post post = createPost(1L, host, "그룹 unreadCount");
            Journey journey = createJourney(30L, post, "그룹 여정");
            ChatRoom room = createGroupRoom(roomId, journey, ChatRoomStatus.ACTIVE);

            ChatMessage message = createMessage(501L, room, host, ChatMessageType.TEXT,
                    "읽음 수 계산", memberCreatedAt.plusMinutes(10));
            ChatMessage readCursor = createMessage(600L, room, readMemberUser, ChatMessageType.TEXT,
                    "읽은 위치", memberCreatedAt.plusMinutes(11));
            ChatRoomMember hostMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);
            ChatRoomMember unreadMember = createMember(room, unreadMemberUser, ChatMemberRole.GUEST, memberCreatedAt);
            ChatRoomMember readMember = createMember(room, readMemberUser, ChatMemberRole.GUEST, readCursor, memberCreatedAt);
            ChatRoomMember newcomerMember = createMember(
                    room,
                    newcomer,
                    ChatMemberRole.GUEST,
                    memberCreatedAt.plusMinutes(20)
            );

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, hostUserId)).thenReturn(Optional.of(hostMember));
            when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(30L, hostUserId, JourneyMemberStatus.ACTIVE))
                    .thenReturn(true);
            when(journeyMemberRepository.countByJourneyIdAndStatus(30L, JourneyMemberStatus.ACTIVE)).thenReturn(4);
            when(chatMessageRepository.findVisibleMessages(eq(roomId), eq(null), eq(memberCreatedAt), any(Pageable.class)))
                    .thenReturn(List.of(message));
            when(chatRoomMemberRepository.findByRoomIdWithLastReadMessage(roomId))
                    .thenReturn(List.of(hostMember, unreadMember, readMember, newcomerMember));

            ChatMessagesResponse response = chatMessageQueryService.retrieveMessages(
                    hostUserId,
                    roomId,
                    new ChatMessageRetrieveRequest(null, 20)
            );

            assertThat(response.messages()).hasSize(1);
            assertThat(response.messages().get(0).messageId()).isEqualTo(message.getId());
            assertThat(response.messages().get(0).unreadCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("검증 실패")
    class ValidationFailure {

        @Test
        @DisplayName("DELETED 방은 찾을 수 없는 방으로 처리한다")
        void deletedRoomThrowsNotFound() {
            Long roomId = 100L;
            User host = createUser(10L, "host", "hostNick");
            ChatRoom room = createPrivateRoom(roomId, createPost(1L, host, "삭제된 방"), ChatRoomStatus.DELETED);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);

            assertThatThrownBy(() -> chatMessageQueryService.retrieveMessages(
                    host.getId(),
                    roomId,
                    new ChatMessageRetrieveRequest(null, 20)
            )).isInstanceOf(ChatRoomNotFoundException.class);

            verify(chatRoomMemberRepository, never()).findByRoomIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 접근 예외가 발생한다")
        void nonMemberThrowsAccessDenied() {
            Long roomId = 100L;
            User host = createUser(10L, "host", "hostNick");
            ChatRoom room = createPrivateRoom(roomId, createPost(1L, host, "권한 없는 방"), ChatRoomStatus.ACTIVE);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, host.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageQueryService.retrieveMessages(
                    host.getId(),
                    roomId,
                    new ChatMessageRetrieveRequest(null, 20)
            )).isInstanceOf(ChatAccessDeniedException.class);
        }

        @Test
        @DisplayName("그룹 방의 active journey member가 아니면 접근 예외가 발생한다")
        void inactiveJourneyMemberThrowsAccessDenied() {
            Long roomId = 200L;
            Long userId = 10L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(userId, "host", "hostNick");
            Journey journey = createJourney(30L, createPost(1L, host, "그룹 채팅 방"), "그룹 여정");
            ChatRoom room = createGroupRoom(roomId, journey, ChatRoomStatus.ACTIVE);
            ChatRoomMember currentMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).thenReturn(Optional.of(currentMember));
            when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(30L, userId, JourneyMemberStatus.ACTIVE))
                    .thenReturn(false);

            assertThatThrownBy(() -> chatMessageQueryService.retrieveMessages(
                    userId,
                    roomId,
                    new ChatMessageRetrieveRequest(null, 20)
            )).isInstanceOf(ChatAccessDeniedException.class);

            verify(chatMessageRepository, never()).findVisibleMessages(any(), any(), any(), any());
        }

        @Test
        @DisplayName("커서 메시지가 현재 방 소속이 아니면 메시지 not found 예외가 발생한다")
        void cursorFromOtherRoomThrowsNotFound() {
            Long roomId = 100L;
            Long userId = 10L;
            Long beforeMessageId = 999L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(userId, "host", "hostNick");
            ChatRoom room = createPrivateRoom(roomId, createPost(1L, host, "커서 검증 방"), ChatRoomStatus.ACTIVE);
            ChatRoomMember currentMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).thenReturn(Optional.of(currentMember));
            when(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                    beforeMessageId,
                    roomId,
                    memberCreatedAt
            )).thenReturn(false);

            assertThatThrownBy(() -> chatMessageQueryService.retrieveMessages(
                    userId,
                    roomId,
                    new ChatMessageRetrieveRequest(beforeMessageId, 20)
            )).isInstanceOf(ChatMessageNotFoundException.class);
        }

        @Test
        @DisplayName("커서 메시지가 입장 전 메시지이면 메시지 not found 예외가 발생한다")
        void cursorBeforeMemberJoinedThrowsNotFound() {
            Long roomId = 100L;
            Long userId = 10L;
            Long beforeMessageId = 999L;
            LocalDateTime memberCreatedAt = LocalDateTime.of(2026, 5, 9, 9, 0);
            User host = createUser(userId, "host", "hostNick");
            ChatRoom room = createPrivateRoom(roomId, createPost(1L, host, "커서 검증 방"), ChatRoomStatus.ACTIVE);
            ChatRoomMember currentMember = createMember(room, host, ChatMemberRole.HOST, memberCreatedAt);

            when(chatRoomRepository.getByIdWithContextOrThrow(roomId)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)).thenReturn(Optional.of(currentMember));
            when(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                    beforeMessageId,
                    roomId,
                    memberCreatedAt
            )).thenReturn(false);

            assertThatThrownBy(() -> chatMessageQueryService.retrieveMessages(
                    userId,
                    roomId,
                    new ChatMessageRetrieveRequest(beforeMessageId, 20)
            )).isInstanceOf(ChatMessageNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("사진·투표 모아보기")
    class RetrieveContents {

        private final LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 1, 10, 0);
        private User user;
        private Journey journey;
        private ChatRoom room;
        private ChatRoomMember member;

        @BeforeEach
        void prepareRoom() {
            user = createUser(10L, "host", "hostNick");
            journey = createJourney(30L, createPost(1L, user, "제주 동행 여행"), "제주 동행 여행");
            room = createGroupRoom(200L, journey, ChatRoomStatus.ACTIVE);
            member = createMember(room, user, ChatMemberRole.HOST, joinedAt);
        }

        @Test
        @DisplayName("사진은 최신순으로 반환하고 마지막 반환 메시지를 커서로 사용하며 읽음 위치를 변경하지 않는다")
        void imagesUseMessageCursorWithoutMarkingRead() {
            allowGroupAccess();
            ChatMessage newest = image(103L);
            ChatMessage older = image(102L);
            when(chatMessageRepository.findVisibleMessagesByType(
                    eq(200L), eq(ChatMessageType.IMAGE), eq(null), eq(joinedAt), any(Pageable.class)))
                    .thenReturn(List.of(newest, older, image(101L)));

            ChatImageListResponse response = chatMessageQueryService.retrieveImages(
                    10L, 200L, new ChatContentListRequest(null, 2));

            assertThat(response.images()).extracting("messageId").containsExactly(103L, 102L);
            assertThat(response.images().get(0).imageUrl()).isEqualTo(newest.getContent());
            assertThat(response.images().get(0).createdAt()).isEqualTo(joinedAt);
            assertThat(response.nextCursor()).isEqualTo(102L);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.size()).isEqualTo(2);
            assertThat(member.getLastReadMessage()).isNull();
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(chatMessageRepository).findVisibleMessagesByType(
                    eq(200L), eq(ChatMessageType.IMAGE), eq(null), eq(joinedAt), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
            verifyNoInteractions(journeyPollQueryService, userRepository, chatSystemMessageFactory);
        }

        @Test
        @DisplayName("사진 다음 페이지는 커서를 검증하고 기본 크기 20으로 조회한다")
        void imagesValidateCursorAndUseDefaultSize() {
            allowGroupAccess();
            when(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(104L, 200L, joinedAt))
                    .thenReturn(true);
            when(chatMessageRepository.findVisibleMessagesByType(
                    eq(200L), eq(ChatMessageType.IMAGE), eq(104L), eq(joinedAt), any(Pageable.class)))
                    .thenReturn(List.of(image(103L)));

            ChatImageListResponse response = chatMessageQueryService.retrieveImages(
                    10L, 200L, new ChatContentListRequest(104L, null));

            assertThat(response.nextCursor()).isNull();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.size()).isEqualTo(1);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(chatMessageRepository).findVisibleMessagesByType(
                    eq(200L), eq(ChatMessageType.IMAGE), eq(104L), eq(joinedAt), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(21);
        }

        @Test
        @DisplayName("투표는 최신 집계를 재사용하고 pollId가 아닌 messageId를 다음 커서로 반환한다")
        void pollsUseLatestSummaryAndMessageCursor() {
            allowGroupAccess();
            ChatMessage newest = pollMessage(2000L, 5L);
            ChatMessage older = pollMessage(1999L, 4L);
            ChatMessage lookAhead = pollMessage(1998L, 3L);
            JourneyPollSummaryResponse open = JourneyPollSummaryResponse.from(
                    newest.getPoll(), joinedAt, 2, List.of(), List.of());
            JourneyPollSummaryResponse closed = JourneyPollSummaryResponse.from(
                    older.getPoll(), joinedAt.plusDays(2), 3, List.of(), List.of());
            when(chatMessageRepository.findVisibleMessagesByType(
                    eq(200L), eq(ChatMessageType.POLL), eq(null), eq(joinedAt), any(Pageable.class)))
                    .thenReturn(List.of(newest, older, lookAhead));
            when(journeyPollQueryService.buildSummaries(List.of(newest.getPoll(), older.getPoll()), 10L))
                    .thenReturn(Map.of(5L, open, 4L, closed));

            ChatPollListResponse response = chatMessageQueryService.retrievePolls(
                    10L, 200L, new ChatContentListRequest(null, 2));

            assertThat(response.polls()).extracting("pollId").containsExactly(5L, 4L);
            assertThat(response.polls()).extracting("messageId").containsExactly(2000L, 1999L);
            assertThat(response.polls().get(0).voterCount()).isEqualTo(2);
            assertThat(response.polls().get(0).journeyId()).isEqualTo(30L);
            assertThat(response.polls().get(0).title()).isEqualTo(open.title());
            assertThat(response.polls().get(1).status()).isEqualTo(closed.status());
            assertThat(response.polls().get(1).closesAt()).isEqualTo(closed.closesAt());
            assertThat(response.nextCursor()).isEqualTo(1999L);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.size()).isEqualTo(2);
            assertThat(member.getLastReadMessage()).isNull();
        }

        @Test
        @DisplayName("빈 투표 목록은 추가 집계 없이 빈 페이지를 반환한다")
        void emptyPollsSkipAggregation() {
            allowGroupAccess();

            ChatPollListResponse response = chatMessageQueryService.retrievePolls(
                    10L, 200L, new ChatContentListRequest(null, 2));

            assertThat(response.polls()).isEmpty();
            assertThat(response.nextCursor()).isNull();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.size()).isZero();
            verifyNoInteractions(journeyPollQueryService);
        }

        @Test
        @DisplayName("1:1 방 사진은 조회할 수 있지만 투표 목록은 빈 목록이다")
        void privateRoomSupportsImagesOnly() {
            ChatRoom privateRoom = createPrivateRoom(200L, journey.getPost(), ChatRoomStatus.ACTIVE);
            ChatRoomMember privateMember = createMember(privateRoom, user, ChatMemberRole.HOST, joinedAt);
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(privateRoom);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(200L, 10L)).thenReturn(Optional.of(privateMember));

            assertThat(chatMessageQueryService.retrieveImages(10L, 200L, new ChatContentListRequest(null, 5)).images())
                    .isEmpty();
            ChatPollListResponse response = chatMessageQueryService.retrievePolls(
                    10L, 200L, new ChatContentListRequest(null, 2));

            assertThat(response.polls()).isEmpty();
            assertThat(response.hasNext()).isFalse();
            verify(chatMessageRepository, never()).findVisibleMessagesByType(
                    any(), eq(ChatMessageType.POLL), any(), any(), any());
            verifyNoInteractions(journeyMemberRepository, journeyPollQueryService);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
        @DisplayName("종료된 방도 현재 참여자는 모아보기를 조회할 수 있다")
        void closedRoomAllowsContentRead(ChatMessageType type) {
            allowGroupAccess();
            ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);

            retrieve(type, new ChatContentListRequest(null, 2));

            verify(chatMessageRepository).findVisibleMessagesByType(
                    eq(200L), eq(type), eq(null), eq(joinedAt), any(Pageable.class));
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
        @DisplayName("삭제된 방에서는 모아보기를 조회할 수 없다")
        void deletedRoomRejectsContentRead(ChatMessageType type) {
            ReflectionTestUtils.setField(room, "status", ChatRoomStatus.DELETED);
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(room);

            assertThatThrownBy(() -> retrieve(type, new ChatContentListRequest(null, 2)))
                    .isInstanceOf(ChatRoomNotFoundException.class);
            verifyNoInteractions(chatMessageRepository, journeyPollQueryService);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
        @DisplayName("채팅방 비참여자는 모아보기를 조회할 수 없다")
        void nonMemberRejectsContentRead(ChatMessageType type) {
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(room);

            assertThatThrownBy(() -> retrieve(type, new ChatContentListRequest(null, 2)))
                    .isInstanceOf(ChatAccessDeniedException.class);
            verifyNoInteractions(chatMessageRepository, journeyPollQueryService);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
        @DisplayName("비활성 여정 참여자는 그룹방 모아보기를 조회할 수 없다")
        void inactiveJourneyMemberRejectsContentRead(ChatMessageType type) {
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(200L, 10L)).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> retrieve(type, new ChatContentListRequest(null, 2)))
                    .isInstanceOf(ChatAccessDeniedException.class);
            verifyNoInteractions(chatMessageRepository, journeyPollQueryService);
        }

        @ParameterizedTest
        @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
        @DisplayName("다른 방 또는 참여 이전 메시지 커서는 거부한다")
        void invisibleCursorRejectsContentRead(ChatMessageType type) {
            allowGroupAccess();

            assertThatThrownBy(() -> retrieve(type, new ChatContentListRequest(999L, 2)))
                    .isInstanceOf(ChatMessageNotFoundException.class);
            verify(chatMessageRepository).existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(999L, 200L, joinedAt);
            verify(chatMessageRepository, never()).findVisibleMessagesByType(any(), any(), any(), any(), any());
        }

        private void retrieve(ChatMessageType type, ChatContentListRequest request) {
            if (type == ChatMessageType.IMAGE) {
                chatMessageQueryService.retrieveImages(10L, 200L, request);
            } else {
                chatMessageQueryService.retrievePolls(10L, 200L, request);
            }
        }

        private void allowGroupAccess() {
            when(chatRoomRepository.getByIdWithContextOrThrow(200L)).thenReturn(room);
            when(chatRoomMemberRepository.findByRoomIdAndUserId(200L, 10L)).thenReturn(Optional.of(member));
            when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(30L, 10L, JourneyMemberStatus.ACTIVE))
                    .thenReturn(true);
        }

        private ChatMessage image(Long id) {
            return createMessage(id, room, user, ChatMessageType.IMAGE, "https://example.com/" + id + ".png", joinedAt);
        }

        private ChatMessage pollMessage(Long messageId, Long pollId) {
            JourneyPoll poll = JourneyPoll.create(journey, user, "Dinner", true, false, false,
                    joinedAt.plusDays(1), joinedAt);
            ReflectionTestUtils.setField(poll, "id", pollId);
            ChatMessage message = ChatMessage.forPoll(room, poll);
            ReflectionTestUtils.setField(message, "id", messageId);
            ReflectionTestUtils.setField(message, "createdAt", joinedAt);
            return message;
        }
    }

    private ChatRoom createPrivateRoom(Long roomId, Post post, ChatRoomStatus status) {
        ChatRoom room = ChatRoom.forPrivateChat(post);
        ReflectionTestUtils.setField(room, "id", roomId);
        ReflectionTestUtils.setField(room, "status", status);
        return room;
    }

    private ChatRoom createGroupRoom(Long roomId, Journey journey, ChatRoomStatus status) {
        ChatRoom room = ChatRoom.createGroupChat(journey);
        ReflectionTestUtils.setField(room, "id", roomId);
        ReflectionTestUtils.setField(room, "status", status);
        return room;
    }

    private ChatRoomMember createMember(ChatRoom room, User user, ChatMemberRole role, LocalDateTime createdAt) {
        return createMember(room, user, role, null, createdAt);
    }

    private ChatRoomMember createMember(
            ChatRoom room,
            User user,
            ChatMemberRole role,
            ChatMessage lastReadMessage,
            LocalDateTime createdAt
    ) {
        ChatRoomMember member = ChatRoomMember.create(room, user, role);
        ReflectionTestUtils.setField(member, "lastReadMessage", lastReadMessage);
        ReflectionTestUtils.setField(member, "createdAt", createdAt);
        return member;
    }

    private ChatMessage createMessage(
            Long messageId,
            ChatRoom room,
            User sender,
            ChatMessageType messageType,
            String content,
            LocalDateTime createdAt
    ) {
        ChatMessage message = ChatMessage.create(room, sender, messageType, content);
        ReflectionTestUtils.setField(message, "id", messageId);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }

    private Journey createJourney(Long journeyId, Post post, String title) {
        Journey journey = Journey.create(post);
        ReflectionTestUtils.setField(journey, "id", journeyId);
        ReflectionTestUtils.setField(journey, "title", title);
        return journey;
    }

    private Post createPost(Long postId, User owner, String title) {
        Post post = Post.createPost(
                owner,
                null,
                title,
                "채팅 메시지 조회 테스트용 본문입니다.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                4,
                LocalDate.now().plusDays(1),
                Gender.U,
                true,
                null,
                null,
                "https://example.com/photo.png",
                "[]",
                null
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private User createUser(Long userId, String name, String nickname) {
        User user = User.builder()
                .email(name + userId + "@example.com")
                .name(name)
                .oauthId("oauth-" + userId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Profile profile = new Profile(user);
        profile.updateNickname(nickname);
        ReflectionTestUtils.setField(user, "profile", profile);
        return user;
    }
}
