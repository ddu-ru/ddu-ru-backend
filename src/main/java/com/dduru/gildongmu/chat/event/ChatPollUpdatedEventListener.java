package com.dduru.gildongmu.chat.event;

import com.dduru.gildongmu.chat.constants.ChatDestinationPaths;
import com.dduru.gildongmu.chat.dto.ws.ChatPollUpdatedPayload;
import com.dduru.gildongmu.chat.repository.ChatRoomRepository;
import com.dduru.gildongmu.journey.event.JourneyPollUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatPollUpdatedEventListener {
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JourneyPollUpdatedEvent event) {
        for (Long roomId : chatRoomRepository.findActiveGroupRoomIdsByJourneyId(event.journeyId())) {
            messagingTemplate.convertAndSend(ChatDestinationPaths.topicRoom(roomId),
                    ChatPollUpdatedPayload.of(roomId, event.journeyId(), event.pollId()));
        }
    }
}
