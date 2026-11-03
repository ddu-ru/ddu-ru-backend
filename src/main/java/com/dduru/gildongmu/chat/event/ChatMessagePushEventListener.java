package com.dduru.gildongmu.chat.event;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.repository.ChatMessageRepository;
import com.dduru.gildongmu.chat.repository.ChatRoomMemberRepository;
import com.dduru.gildongmu.chat.service.ChatOnlineStatusService;
import com.dduru.gildongmu.chat.service.ChatPushNotificationService;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ChatMessagePushEventListener {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatOnlineStatusService chatOnlineStatusService;
    private final ChatPushNotificationService chatPushNotificationService;
    private final UserRepository userRepository;

    // Spring 6: @TransactionalEventListener + @Transactional 동시 사용 금지 (RestrictedTransactionalEventListenerFactory)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageCreatedEvent event) {
        ChatMessage message = chatMessageRepository.findByIdWithSenderAndRoom(event.messageId())
                .orElse(null);
        if (message == null) return;

        if (message.getMessageType() == ChatMessageType.SYSTEM) return;

        Long senderId = message.getSender().getId();
        Long roomId = event.roomId();

        List<Long> recipientIds = chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(roomId)
                .stream()
                .filter(id -> !id.equals(senderId))
                .toList();
        if (recipientIds.isEmpty()) return;

        Set<Long> onlineIds = chatOnlineStatusService.getOnlineUserIds(roomId);
        List<Long> offlineRecipients = recipientIds.stream()
                .filter(id -> !onlineIds.contains(id))
                .toList();
        if (offlineRecipients.isEmpty()) return;

        List<Long> fcmTargetIds = userRepository.findEnabledUserIds(offlineRecipients);
        if (fcmTargetIds.isEmpty()) return;

        if (!chatPushNotificationService.canSendPush(roomId)) return;

        String roomTitle = resolveRoomTitle(message);
        String senderNickname = message.getSender().getProfile().getNickname();

        chatPushNotificationService.sendPush(
                fcmTargetIds, roomTitle, senderNickname,
                message.getContent(), message.getMessageType(), message.getRoom().getRoomType(), roomId
        );
    }

    private String resolveRoomTitle(ChatMessage message) {
        ChatRoom room = message.getRoom();
        if (room.getRoomType() == ChatRoomType.GROUP) {
            return room.getJourney().getTitle();
        }
        // PRIVATE: 수신자 입장에서 상대방 = 발신자
        return message.getSender().getProfile().getNickname();
    }
}
