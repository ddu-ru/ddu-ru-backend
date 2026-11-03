package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.request.ChatNotificationSettingsRequest;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("채팅방 알림 설정 서비스")
class ChatNotificationSettingsServiceTest {

    @Mock private ChatRoomRepository roomRepository;
    @Mock private ChatRoomMemberRepository memberRepository;
    @Mock private JourneyMemberRepository journeyMemberRepository;
    @Mock private ChatRoom room;
    @InjectMocks private ChatNotificationSettingsService service;

    private ChatRoomMember member;

    @BeforeEach
    void setUp() {
        member = ChatRoomMember.create(room, null, ChatMemberRole.GUEST);
        when(roomRepository.getByIdWithContextOrThrow(1L)).thenReturn(room);
    }

    @ParameterizedTest
    @EnumSource(value = ChatRoomStatus.class, names = {"ACTIVE", "CLOSED"})
    @DisplayName("활성·종료 방에서 기본 ON 조회 및 OFF/ON 멱등 변경을 허용한다")
    void retrievesAndUpdates(ChatRoomStatus status) {
        when(room.getStatus()).thenReturn(status);
        when(room.getRoomType()).thenReturn(ChatRoomType.PRIVATE);
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(member));
        when(memberRepository.findByRoomIdAndUserIdWithLock(1L, 10L)).thenReturn(Optional.of(member));

        assertThat(service.retrieve(10L, 1L).enabled()).isTrue();
        assertThat(service.update(10L, 1L, new ChatNotificationSettingsRequest(false)).enabled()).isFalse();
        assertThat(service.update(10L, 1L, new ChatNotificationSettingsRequest(false)).enabled()).isFalse();
        assertThat(service.retrieve(10L, 1L).enabled()).isFalse();
        assertThat(service.update(10L, 1L, new ChatNotificationSettingsRequest(true)).enabled()).isTrue();
        verifyNoInteractions(journeyMemberRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("존재하지 않는 방은 조회·변경할 수 없다")
    void missingRoom(boolean update) {
        when(roomRepository.getByIdWithContextOrThrow(1L)).thenThrow(new ChatRoomNotFoundException());
        assertThatThrownBy(() -> invoke(update)).isInstanceOf(ChatRoomNotFoundException.class);
        verifyNoInteractions(memberRepository, journeyMemberRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("삭제된 방은 조회·변경할 수 없다")
    void deletedRoom(boolean update) {
        when(room.getStatus()).thenReturn(ChatRoomStatus.DELETED);
        assertThatThrownBy(() -> invoke(update)).isInstanceOf(ChatRoomNotFoundException.class);
        verifyNoInteractions(memberRepository, journeyMemberRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("비멤버는 조회·변경할 수 없다")
    void nonMember(boolean update) {
        when(room.getStatus()).thenReturn(ChatRoomStatus.ACTIVE);
        assertThatThrownBy(() -> invoke(update)).isInstanceOf(ChatAccessDeniedException.class);
        verifyNoInteractions(journeyMemberRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("채팅 멤버여도 ACTIVE 여정 멤버가 아니면 조회·변경할 수 없다")
    void inactiveJourneyMember(boolean update) {
        setupGroup(update);
        assertThatThrownBy(() -> invoke(update)).isInstanceOf(ChatAccessDeniedException.class);
        assertThat(member.isNotificationEnabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("종료된 그룹방도 ACTIVE 여정 멤버는 조회·변경할 수 있다")
    void activeJourneyMember(boolean update) {
        setupGroup(update);
        when(journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(20L, 10L, JourneyMemberStatus.ACTIVE))
                .thenReturn(true);
        invoke(update);
        assertThat(member.isNotificationEnabled()).isEqualTo(!update);
    }

    private void setupGroup(boolean update) {
        when(room.getStatus()).thenReturn(ChatRoomStatus.CLOSED);
        when(room.getRoomType()).thenReturn(ChatRoomType.GROUP);
        Journey journey = mock(Journey.class);
        when(journey.getId()).thenReturn(20L);
        when(room.getJourney()).thenReturn(journey);
        if (update) {
            when(memberRepository.findByRoomIdAndUserIdWithLock(1L, 10L)).thenReturn(Optional.of(member));
        } else {
            when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(member));
        }
    }

    private void invoke(boolean update) {
        if (update) {
            service.update(10L, 1L, new ChatNotificationSettingsRequest(false));
        } else {
            service.retrieve(10L, 1L);
        }
    }
}
