package com.dduru.gildongmu.chat.domain;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", unique = true)
    private JourneyPoll poll;

    @Builder(access = AccessLevel.PRIVATE)
    private ChatMessage(ChatRoom room, User sender, ChatMessageType messageType, String content) {
        this.room = room;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
    }

    public static ChatMessage create(ChatRoom room, User sender, ChatMessageType messageType, String content) {
        return ChatMessage.builder()
                .room(room)
                .sender(sender)
                .messageType(messageType)
                .content(content)
                .build();
    }

    public static ChatMessage forPoll(ChatRoom room, JourneyPoll poll) {
        ChatMessage message = create(room, poll.getAuthor(), ChatMessageType.POLL, poll.getTitle());
        message.poll = poll;
        return message;
    }
}
