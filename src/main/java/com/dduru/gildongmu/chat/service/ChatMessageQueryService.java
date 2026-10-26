package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.request.ChatMessageRetrieveRequest;
import com.dduru.gildongmu.chat.dto.response.ChatMessageItemResponse;
import com.dduru.gildongmu.chat.dto.response.ChatMessagePageResponse;
import com.dduru.gildongmu.chat.dto.response.ChatMessagesResponse;
import com.dduru.gildongmu.chat.dto.response.ChatRoomInfoResponse;
import com.dduru.gildongmu.chat.dto.response.ChatMessageSenderResponse;
import com.dduru.gildongmu.chat.dto.ws.ChatSystemMessagePayload;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatMessageNotFoundException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.chat.system.ChatSystemMessageFactory;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.journey.service.JourneyPollQueryService;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.reverse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final JourneyMemberRepository journeyMemberRepository;
    private final UserRepository userRepository;
    private final ChatSystemMessageFactory chatSystemMessageFactory;
    private final JourneyPollQueryService journeyPollQueryService;

    public ChatMessagesResponse retrieveMessages(Long userId, Long roomId, ChatMessageRetrieveRequest request) {
        ChatMessageRetrieveRequest normalizedRequest = normalizeRequest(request);

        ChatRoom room = chatRoomRepository.getByIdWithContextOrThrow(roomId);
        validateRoomRetrievable(room);

        ChatRoomMember currentMember = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        validateGroupAccess(room, userId);
        validateCursor(roomId, normalizedRequest.beforeMessageId(), currentMember.getCreatedAt());

        int size = normalizedRequest.sizeOrDefault();

        List<ChatMessage> fetchedMessages = chatMessageRepository.findVisibleMessages(
                roomId,
                normalizedRequest.beforeMessageId(),
                currentMember.getCreatedAt(),
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = fetchedMessages.size() > size;
        List<ChatMessage> pageMessages = trimLookAheadMessages(fetchedMessages, size);
        Long nextCursor = resolveNextCursor(pageMessages, hasNext);
        List<ChatMessage> orderedMessages = reverseToAscending(pageMessages);

        Map<Long, ChatSystemMessagePayload> systemPayloads = deserializeSystemPayloads(orderedMessages);
        Map<Long, User> systemUsers = loadUsersForSystemMessages(systemPayloads.values());
        Map<Long, Integer> unreadCounts = calculateUnreadCounts(roomId, orderedMessages);
        Map<Long, JourneyPollSummaryResponse> polls = loadPolls(orderedMessages, userId);

        return new ChatMessagesResponse(
                toRoomInfo(room, userId),
                new ChatMessagePageResponse(size, hasNext, nextCursor),
                toMessageResponses(orderedMessages, room, userId, systemPayloads, systemUsers, unreadCounts, polls)
        );
    }

    private static ChatMessageRetrieveRequest normalizeRequest(ChatMessageRetrieveRequest request) {
        if (request == null) {
            return new ChatMessageRetrieveRequest(null, null);
        }
        return request;
    }

    private Map<Long, JourneyPollSummaryResponse> loadPolls(List<ChatMessage> messages, Long userId) {
        List<JourneyPoll> polls = messages.stream().filter(message -> message.getMessageType() == ChatMessageType.POLL)
                .map(ChatMessage::getPoll).toList();
        return polls.isEmpty() ? Map.of() : journeyPollQueryService.buildSummaries(polls, userId);
    }

    private static void validateRoomRetrievable(ChatRoom room) {
        if (room.getStatus() == ChatRoomStatus.DELETED) {
            throw new ChatRoomNotFoundException();
        }
    }

    private void validateGroupAccess(ChatRoom room, Long userId) {
        if (room.getRoomType() != ChatRoomType.GROUP) {
            return;
        }

        boolean activeJourneyMember = journeyMemberRepository.existsByJourneyIdAndUserIdAndStatus(
                room.getJourney().getId(),
                userId,
                JourneyMemberStatus.ACTIVE
        );
        if (!activeJourneyMember) {
            throw new ChatAccessDeniedException();
        }
    }

    private void validateCursor(Long roomId, Long beforeMessageId, LocalDateTime visibleFrom) {
        if (beforeMessageId == null) {
            return;
        }
        if (!chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                beforeMessageId,
                roomId,
                visibleFrom
        )) {
            throw new ChatMessageNotFoundException();
        }
    }

    private static List<ChatMessage> trimLookAheadMessages(List<ChatMessage> messages, int size) {
        if (messages.size() <= size) {
            return messages;
        }
        return new ArrayList<>(messages.subList(0, size));
    }

    private static Long resolveNextCursor(List<ChatMessage> pageMessages, boolean hasNext) {
        if (!hasNext || pageMessages.isEmpty()) {
            return null;
        }
        return pageMessages.get(pageMessages.size() - 1).getId();
    }

    private static List<ChatMessage> reverseToAscending(List<ChatMessage> pageMessages) {
        List<ChatMessage> orderedMessages = new ArrayList<>(pageMessages);
        reverse(orderedMessages);
        return orderedMessages;
    }

    private Map<Long, Integer> calculateUnreadCounts(Long roomId, List<ChatMessage> messages) {
        boolean hasUnreadCountTarget = messages.stream()
                .anyMatch(message -> message.getMessageType() != ChatMessageType.SYSTEM);
        if (!hasUnreadCountTarget) {
            return Map.of();
        }

        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomIdWithLastReadMessage(roomId);
        Map<Long, Integer> unreadCounts = new HashMap<>();

        for (ChatMessage message : messages) {
            if (message.getMessageType() == ChatMessageType.SYSTEM) {
                continue;
            }
            unreadCounts.put(message.getId(), calculateUnreadCount(message, members));
        }
        return unreadCounts;
    }

    private static int calculateUnreadCount(ChatMessage message, List<ChatRoomMember> members) {
        int unreadCount = 0;
        for (ChatRoomMember member : members) {
            if (isSender(message, member) || joinedAfterMessage(member, message) || hasReadMessage(member, message)) {
                continue;
            }
            unreadCount++;
        }
        return unreadCount;
    }

    private static boolean isSender(ChatMessage message, ChatRoomMember member) {
        return message.getSender() != null && message.getSender().getId().equals(member.getUser().getId());
    }

    private static boolean joinedAfterMessage(ChatRoomMember member, ChatMessage message) {
        return member.getCreatedAt().isAfter(message.getCreatedAt());
    }

    private static boolean hasReadMessage(ChatRoomMember member, ChatMessage message) {
        ChatMessage lastReadMessage = member.getLastReadMessage();
        return lastReadMessage != null && lastReadMessage.getId() >= message.getId();
    }

    private ChatRoomInfoResponse toRoomInfo(ChatRoom room, Long userId) {
        return switch (room.getRoomType()) {
            case PRIVATE -> toPrivateRoomInfo(room, userId);
            case GROUP -> toGroupRoomInfo(room);
        };
    }

    private ChatRoomInfoResponse toPrivateRoomInfo(ChatRoom room, Long userId) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomIdWithUserProfile(room.getId());
        String opponentNickname = members.stream()
                .map(ChatRoomMember::getUser)
                .filter(user -> !user.getId().equals(userId))
                .findFirst()
                .map(ChatMessageSenderResponse::displayNameOf)
                .orElse(null);

        return new ChatRoomInfoResponse(
                room.getId(),
                room.getRoomType(),
                room.getStatus() == ChatRoomStatus.ACTIVE,
                room.getPost().getTitle(),
                opponentNickname,
                null,
                null
        );
    }

    private ChatRoomInfoResponse toGroupRoomInfo(ChatRoom room) {
        int participantCount = journeyMemberRepository.countByJourneyIdAndStatus(
                room.getJourney().getId(),
                JourneyMemberStatus.ACTIVE
        );

        return new ChatRoomInfoResponse(
                room.getId(),
                room.getRoomType(),
                room.getStatus() == ChatRoomStatus.ACTIVE,
                null,
                null,
                room.getJourney().getTitle(),
                participantCount
        );
    }

    private Map<Long, ChatSystemMessagePayload> deserializeSystemPayloads(List<ChatMessage> messages) {
        Map<Long, ChatSystemMessagePayload> payloads = new HashMap<>();
        for (ChatMessage message : messages) {
            if (message.getMessageType() == ChatMessageType.SYSTEM) {
                payloads.put(message.getId(), chatSystemMessageFactory.deserialize(message.getContent()));
            }
        }
        return payloads;
    }

    private Map<Long, User> loadUsersForSystemMessages(Collection<ChatSystemMessagePayload> payloads) {
        Set<Long> userIds = collectSystemUserIds(payloads);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllWithProfileByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static Set<Long> collectSystemUserIds(Collection<ChatSystemMessagePayload> payloads) {
        Set<Long> userIds = new HashSet<>();
        for (ChatSystemMessagePayload payload : payloads) {
            addIfNotNull(userIds, payload.actorUserId());
            addIfNotNull(userIds, payload.inviteeUserId());
            addIfNotNull(userIds, payload.userId());
            addIfNotNull(userIds, payload.targetUserId());
        }
        return userIds;
    }

    private static void addIfNotNull(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private static List<ChatMessageItemResponse> toMessageResponses(
            List<ChatMessage> messages,
            ChatRoom room,
            Long currentUserId,
            Map<Long, ChatSystemMessagePayload> systemPayloads,
            Map<Long, User> systemUsers,
            Map<Long, Integer> unreadCounts,
            Map<Long, JourneyPollSummaryResponse> polls
    ) {
        Long hostUserId = room.getContextPost().getUser().getId();
        return messages.stream()
                .map(message -> toMessageResponse(
                        message,
                        hostUserId,
                        currentUserId,
                        systemPayloads,
                        systemUsers,
                        unreadCounts,
                        polls
                ))
                .toList();
    }

    private static ChatMessageItemResponse toMessageResponse(
            ChatMessage message,
            Long hostUserId,
            Long currentUserId,
            Map<Long, ChatSystemMessagePayload> systemPayloads,
            Map<Long, User> systemUsers,
            Map<Long, Integer> unreadCounts,
            Map<Long, JourneyPollSummaryResponse> polls
    ) {
        return switch (message.getMessageType()) {
            case POLL -> ChatMessageItemResponse.forPoll(message, hostUserId, currentUserId,
                    unreadCounts.get(message.getId()), polls.get(message.getPoll().getId()));
            case TEXT -> ChatMessageItemResponse.forText(
                    message,
                    hostUserId,
                    currentUserId,
                    unreadCounts.get(message.getId())
            );
            case IMAGE -> ChatMessageItemResponse.forImage(
                    message,
                    hostUserId,
                    currentUserId,
                    unreadCounts.get(message.getId())
            );
            case SYSTEM -> ChatMessageItemResponse.forSystem(
                    message,
                    systemPayloads.get(message.getId()),
                    systemUsers
            );
        };
    }
}
