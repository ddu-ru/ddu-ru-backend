package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatMessageNotFoundException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.report.exception.InvalidReportTargetException;
import com.dduru.gildongmu.report.exception.SelfReportNotAllowedException;
import com.dduru.gildongmu.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatReportAccessValidator 테스트")
class ChatReportAccessValidatorTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private JourneyMemberRepository journeyMemberRepository;

    @InjectMocks
    private ChatReportAccessValidator chatReportAccessValidator;

    private ChatRoom accessibleRoom() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(7L);
        when(room.getRoomType()).thenReturn(ChatRoomType.PRIVATE);
        when(chatRoomRepository.getByIdWithContextOrThrow(7L)).thenReturn(room);
        ChatRoomMember member = ChatRoomMember.builder().room(room).build();
        ReflectionTestUtils.setField(member, "createdAt", JOINED_AT);
        when(chatRoomMemberRepository.findByRoomIdAndUserId(7L, 1L)).thenReturn(Optional.of(member));
        return room;
    }

    private ChatMessage message(ChatRoom room, ChatMessageType type, Long senderId, LocalDateTime createdAt) {
        User sender = senderId == null ? null : User.builder().name("sender").build();
        if (sender != null) {
            ReflectionTestUtils.setField(sender, "id", senderId);
        }
        ChatMessage message = ChatMessage.create(room, sender, type, "content");
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        when(chatMessageRepository.findById(8L)).thenReturn(Optional.of(message));
        return message;
    }

    @Test
    @DisplayName("참여 시각에 작성된 메시지는 신고할 수 있다")
    void acceptsVisibleMessageAtJoinBoundary() {
        ChatMessage message = message(accessibleRoom(), ChatMessageType.TEXT, 2L, JOINED_AT);
        assertThat(chatReportAccessValidator.getReportableMessage(7L, 8L, 1L)).isSameAs(message);
    }

    @Test
    @DisplayName("참여 이전에 작성된 메시지는 신고할 수 없다")
    void rejectsMessageBeforeJoining() {
        message(accessibleRoom(), ChatMessageType.TEXT, 2L, JOINED_AT.minusSeconds(1));
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(ChatMessageNotFoundException.class);
    }

    @Test
    @DisplayName("다른 채팅방의 메시지는 신고할 수 없다")
    void rejectsMessageFromOtherRoom() {
        accessibleRoom();
        ChatRoom other = mock(ChatRoom.class);
        when(other.getId()).thenReturn(9L);
        message(other, ChatMessageType.TEXT, 2L, JOINED_AT);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(ChatMessageNotFoundException.class);
    }

    @Test
    @DisplayName("시스템 메시지는 신고할 수 없다")
    void rejectsSystemMessage() {
        message(accessibleRoom(), ChatMessageType.SYSTEM, null, JOINED_AT);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(InvalidReportTargetException.class);
    }

    @Test
    @DisplayName("본인의 메시지는 신고할 수 없다")
    void rejectsOwnMessage() {
        message(accessibleRoom(), ChatMessageType.IMAGE, 1L, JOINED_AT);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(SelfReportNotAllowedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 메시지는 신고할 수 없다")
    void rejectsMissingMessage() {
        accessibleRoom();
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 99L, 1L))
                .isInstanceOf(ChatMessageNotFoundException.class);
    }

    @Test
    @DisplayName("채팅방 비참여자는 메시지를 신고할 수 없다")
    void rejectsNonMember() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(7L);
        when(chatRoomRepository.getByIdWithContextOrThrow(7L)).thenReturn(room);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(ChatAccessDeniedException.class);
    }

    @Test
    @DisplayName("삭제된 채팅방의 메시지는 신고할 수 없다")
    void rejectsDeletedRoom() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getStatus()).thenReturn(ChatRoomStatus.DELETED);
        when(chatRoomRepository.getByIdWithContextOrThrow(7L)).thenReturn(room);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(ChatRoomNotFoundException.class);
    }

    @Test
    @DisplayName("비활성 여정 참여자는 그룹방의 메시지를 신고할 수 없다")
    void rejectsInactiveJourneyMember() {
        ChatRoom room = accessibleRoom();
        Journey journey = mock(Journey.class);
        when(room.getRoomType()).thenReturn(ChatRoomType.GROUP);
        when(room.getJourney()).thenReturn(journey);
        when(journey.getId()).thenReturn(3L);
        assertThatThrownBy(() -> chatReportAccessValidator.getReportableMessage(7L, 8L, 1L))
                .isInstanceOf(ChatAccessDeniedException.class);
        verify(journeyMemberRepository).existsByJourneyIdAndUserIdAndStatus(3L, 1L, JourneyMemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("종료된 채팅방에서도 기존 참여자는 메시지를 신고할 수 있다")
    void closedRoomStillAllowsReportsForExistingMembers() {
        ChatRoom room = accessibleRoom();
        when(room.getStatus()).thenReturn(ChatRoomStatus.CLOSED);
        ChatMessage message = message(room, ChatMessageType.TEXT, 2L, JOINED_AT);
        assertThat(chatReportAccessValidator.getReportableMessage(7L, 8L, 1L)).isSameAs(message);
    }
}
