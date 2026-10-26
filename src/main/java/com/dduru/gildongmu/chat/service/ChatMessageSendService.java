package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.constants.ChatDestinationPaths;
import com.dduru.gildongmu.chat.constants.ChatMessageConstants;
import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomStatus;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.ws.ChatMessageBroadcastPayload;
import com.dduru.gildongmu.chat.dto.ws.ChatMessageSendRequest;
import com.dduru.gildongmu.chat.dto.ws.ChatSystemMessagePayload;
import com.dduru.gildongmu.chat.event.ChatMessageCreatedEvent;
import com.dduru.gildongmu.chat.exception.ChatAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatRoomClosedException;
import com.dduru.gildongmu.chat.exception.ChatSystemMessageSendAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatPollMessageSendAccessDeniedException;
import com.dduru.gildongmu.chat.exception.ChatRoomNotFoundException;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.chat.system.ChatSystemMessageFactory;
import com.dduru.gildongmu.chat.validation.ChatTextValidator;
import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.dto.response.JourneyPollSummaryResponse;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageSendService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private final ProfileImageResolver profileImageResolver;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatSystemMessageFactory chatSystemMessageFactory;
    private final S3ImageUrlValidator s3ImageUrlValidator;
    private final ApplicationEventPublisher eventPublisher;

    public void sendUserMessage(Long senderUserId, Long roomId, ChatMessageSendRequest request) {
        ChatRoom room = chatRoomRepository.getByIdOrThrow(roomId);
        validateRoomOpen(room);
        ChatRoomMember senderMember = getSenderMember(senderUserId, roomId);

        User sender = userRepository.getWithProfileByIdOrThrow(senderUserId);
        String content = resolveUserMessageContent(request);
        saveUserMessageAndBroadcast(room, sender, senderMember, request.messageType(), content);
    }

    public void publishUserInvited(ChatRoom room, long inviteeUserId, long actorUserId) {
        saveSystemMessageAndBroadcast(room, chatSystemMessageFactory.userInvited(inviteeUserId, actorUserId));
    }

    public void publishPoll(JourneyPoll poll, JourneyPollSummaryResponse publicSummary) {
        ChatRoom room = chatRoomRepository.findByJourneyIdAndRoomType(poll.getJourney().getId(), ChatRoomType.GROUP)
                .orElseThrow(ChatRoomNotFoundException::new);
        validateRoomOpen(room);
        ChatRoomMember senderMember = getSenderMember(poll.getAuthor().getId(), room.getId());
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.forPoll(room, poll));
        senderMember.readUpTo(message);
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(room.getId(), message.getId()));
        broadcastAfterCommit(ChatMessageBroadcastPayload.ofPollMessage(message, room.getContextPost(),
                profileImageResolver, publicSummary), room.getId());
    }

    public void publishUserKicked(ChatRoom room, long targetUserId, long actorUserId) {
        saveSystemMessageAndBroadcast(room, chatSystemMessageFactory.userKicked(targetUserId, actorUserId));
    }

    private void saveSystemMessageAndBroadcast(ChatRoom room, ChatSystemMessagePayload systemMessage) {
        ChatMessage chatMessage = saveAndFlushMessage(room, null, ChatMessageType.SYSTEM,
                chatSystemMessageFactory.serialize(systemMessage)
        );
        broadcastAfterCommit(toPayload(chatMessage), room.getId());
    }

    private ChatRoomMember getSenderMember(Long senderUserId, Long roomId) {
        return chatRoomMemberRepository.findByRoomIdAndUserIdWithLock(roomId, senderUserId)
                .orElseThrow(ChatAccessDeniedException::new);
    }

    private void saveUserMessageAndBroadcast(
            ChatRoom room,
            User sender,
            ChatRoomMember senderMember,
            ChatMessageType messageType,
            String content
    ) {
        ChatMessage chatMessage = saveAndFlushMessage(room, sender, messageType, content);
        senderMember.readUpTo(chatMessage);
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(room.getId(), chatMessage.getId()));
        broadcastAfterCommit(toPayload(chatMessage), room.getId());
    }

    private ChatMessage saveAndFlushMessage(ChatRoom room, User sender, ChatMessageType messageType, String content) {
        ChatMessage message = chatMessageRepository.save(ChatMessage.create(room, sender, messageType, content));
        chatMessageRepository.flush();
        return message;
    }

    private ChatMessageBroadcastPayload toPayload(ChatMessage message) {
        Post post = message.getRoom().getContextPost();

        if (message.getMessageType() == ChatMessageType.SYSTEM) {
            return ChatMessageBroadcastPayload.ofSystemMessage(message, post, chatSystemMessageFactory);
        }

        return ChatMessageBroadcastPayload.ofUserMessage(message, post, message.getSender(), profileImageResolver);
    }

    private String resolveUserMessageContent(ChatMessageSendRequest request) {
        return switch (request.messageType()) {
            case TEXT -> ChatTextValidator.validateAndNormalize(request.content());
            case IMAGE -> s3ImageUrlValidator.validateAndNormalize(
                    request.content(),
                    S3ImageDirectory.CHATS,
                    ChatMessageConstants.MAX_IMAGE_URL_LENGTH
            );
            case SYSTEM -> throw new ChatSystemMessageSendAccessDeniedException();
            case POLL -> throw new ChatPollMessageSendAccessDeniedException();
        };
    }

    private static void validateRoomOpen(ChatRoom room) {
        if (room.getStatus() == ChatRoomStatus.CLOSED || room.getStatus() == ChatRoomStatus.DELETED) {
            throw new ChatRoomClosedException();
        }
    }

    /**
     * 트랜잭션이 커밋된 뒤에만 브로드캐스트해서, 롤백 시 잘못된 실시간 메시지가 나가지 않게 한다.
     * 테스트 등 비트랜잭션 호출에서는 즉시 전송한다.
     */
    private void broadcastAfterCommit(ChatMessageBroadcastPayload payload, Long roomId) {
        String destination = ChatDestinationPaths.topicRoom(roomId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            simpMessagingTemplate.convertAndSend(destination, payload);
                        }
                    });
        } else {
            simpMessagingTemplate.convertAndSend(destination, payload);
        }
    }
}
