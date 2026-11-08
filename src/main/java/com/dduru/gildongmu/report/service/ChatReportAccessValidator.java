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
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.report.exception.InvalidReportTargetException;
import com.dduru.gildongmu.report.exception.SelfReportNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatReportAccessValidator {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final JourneyMemberRepository journeyMemberRepository;

    public ChatMessage getReportableMessage(Long roomId, Long messageId, Long userId) {
        ChatRoom room = chatRoomRepository.getByIdWithContextOrThrow(roomId);
        ChatRoomMember member = validateMember(room, userId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(ChatMessageNotFoundException::new);
        if (!message.getRoom().getId().equals(roomId)
                || message.getCreatedAt().isBefore(member.getCreatedAt())) {
            throw new ChatMessageNotFoundException();
        }
        if (message.getMessageType() == ChatMessageType.SYSTEM || message.getSender() == null) {
            throw new InvalidReportTargetException();
        }
        if (message.getSender().getId().equals(userId)) {
            throw new SelfReportNotAllowedException();
        }
        return message;
    }

    private ChatRoomMember validateMember(ChatRoom room, Long userId) {
        if (room.getStatus() == ChatRoomStatus.DELETED) {
            throw new ChatRoomNotFoundException();
        }
        ChatRoomMember member = chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
                .orElseThrow(ChatAccessDeniedException::new);
        if (room.getRoomType() == ChatRoomType.GROUP
                && !journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(
                        room.getJourney().getId(), userId, JourneyMemberStatus.ACTIVE)) {
            throw new ChatAccessDeniedException();
        }
        return member;
    }
}
