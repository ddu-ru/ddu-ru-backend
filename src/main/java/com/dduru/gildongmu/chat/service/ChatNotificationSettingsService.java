package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.request.ChatNotificationSettingsRequest;
import com.dduru.gildongmu.chat.dto.response.ChatNotificationSettingsResponse;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatNotificationSettingsService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final JourneyMemberRepository journeyMemberRepository;

    public ChatNotificationSettingsResponse retrieve(Long userId, Long roomId) {
        ChatRoom room = getRoom(roomId);
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        validateGroupAccess(room, userId);
        return new ChatNotificationSettingsResponse(roomId, member.isNotificationEnabled());
    }

    @Transactional
    public ChatNotificationSettingsResponse update(Long userId, Long roomId, ChatNotificationSettingsRequest request) {
        ChatRoom room = getRoom(roomId);
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserIdWithLock(roomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        validateGroupAccess(room, userId);
        member.changeNotificationSettings(request.enabled());
        return new ChatNotificationSettingsResponse(roomId, member.isNotificationEnabled());
    }

    private ChatRoom getRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.getByIdWithContextOrThrow(roomId);
        if (room.getStatus() == ChatRoomStatus.DELETED) {
            throw new ChatRoomNotFoundException();
        }
        return room;
    }

    private void validateGroupAccess(ChatRoom room, Long userId) {
        if (room.getRoomType() == ChatRoomType.GROUP
                && !journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(
                        room.getJourney().getId(), userId, JourneyMemberStatus.ACTIVE)) {
            throw new ChatAccessDeniedException();
        }
    }
}
