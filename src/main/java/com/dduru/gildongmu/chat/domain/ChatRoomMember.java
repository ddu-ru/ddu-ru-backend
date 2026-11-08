package com.dduru.gildongmu.chat.domain;

import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    @Builder
    public ChatRoomMember(ChatRoom room, User user, ChatMemberRole role, ChatMessage lastReadMessage) {
        this.room = room;
        this.user = user;
        this.role = role;
        this.lastReadMessage = lastReadMessage;
    }

    public static ChatRoomMember create(ChatRoom room, User user, ChatMemberRole role) {
        return ChatRoomMember.builder()
                .room(room)
                .user(user)
                .role(role)
                .build();
    }

    public boolean readUpTo(ChatMessage message) {
        if (lastReadMessage != null && lastReadMessage.getId() >= message.getId()) {
            return false;
        }

        this.lastReadMessage = message;
        return true;
    }

    public void changeNotificationSettings(boolean enabled) {
        this.notificationEnabled = enabled;
    }
}
