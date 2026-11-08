package com.dduru.gildongmu.chat.repository;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import com.dduru.gildongmu.chat.domain.enums.ChatMemberRole;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.chat.dto.request.ChatNotificationSettingsRequest;
import com.dduru.gildongmu.chat.event.ChatMessageCreatedEvent;
import com.dduru.gildongmu.chat.event.ChatMessagePushEventListener;
import com.dduru.gildongmu.chat.service.ChatNotificationSettingsService;
import com.dduru.gildongmu.chat.service.ChatOnlineStatusService;
import com.dduru.gildongmu.chat.service.ChatPushNotificationService;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyMember;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class, ChatNotificationSettingsService.class})
@DisplayName("ChatRoomMemberRepository 테스트")
class ChatRoomMemberRepositoryTest {

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatNotificationSettingsService settingsService;

    @Test
    @DisplayName("findByRoomIdWithLastReadMessage는 현재 방 멤버와 lastReadMessage를 함께 조회한다")
    void findByRoomIdWithLastReadMessageFetchesReadCursor() {
        User host = persistUser("host", 1);
        User guest = persistUser("guest", 2);
        Destination destination = persistDestination();
        Post post = persistPost(host, destination);
        ChatRoom room = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatMessage message = entityManager.persist(ChatMessage.create(room, host, ChatMessageType.TEXT, "target"));
        ChatMessage otherMessage = entityManager.persist(ChatMessage.create(otherRoom, guest, ChatMessageType.TEXT, "other"));

        ChatRoomMember hostMember = entityManager.persist(ChatRoomMember.builder()
                .room(room)
                .user(host)
                .role(ChatMemberRole.HOST)
                .lastReadMessage(message)
                .build());
        entityManager.persist(ChatRoomMember.builder()
                .room(room)
                .user(guest)
                .role(ChatMemberRole.GUEST)
                .build());
        entityManager.persist(ChatRoomMember.builder()
                .room(otherRoom)
                .user(host)
                .role(ChatMemberRole.HOST)
                .lastReadMessage(otherMessage)
                .build());
        entityManager.flush();
        entityManager.clear();

        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomIdWithLastReadMessage(room.getId());

        assertThat(members).hasSize(2);
        assertThat(members).extracting(member -> member.getUser().getId())
                .containsExactlyInAnyOrder(host.getId(), guest.getId());
        ChatRoomMember foundHost = members.stream()
                .filter(member -> member.getUser().getId().equals(hostMember.getUser().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(foundHost.getLastReadMessage().getId()).isEqualTo(message.getId());
    }

    @Test
    @DisplayName("설정은 사용자·방별로 저장되며 읽음 위치와 다른 멤버에 영향을 주지 않고 재참여 시 ON이다")
    void notificationSettingsPersistIndependentlyAndResetOnRejoin() {
        User host = persistUser("host", 1);
        User guest = persistUser("guest", 2);
        Post post = persistPost(host, persistDestination());
        ChatRoom room = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatMessage message = entityManager.persist(ChatMessage.create(room, host, ChatMessageType.TEXT, "읽은 메시지"));
        entityManager.persist(ChatRoomMember.create(room, host, ChatMemberRole.HOST));
        ChatRoomMember guestMember = entityManager.persist(ChatRoomMember.create(room, guest, ChatMemberRole.GUEST));
        guestMember.readUpTo(message);
        entityManager.persist(ChatRoomMember.create(otherRoom, guest, ChatMemberRole.GUEST));
        entityManager.flush();
        entityManager.clear();

        assertThat(settingsService.retrieve(guest.getId(), room.getId()).enabled()).isTrue();
        settingsService.update(guest.getId(), room.getId(),
                new ChatNotificationSettingsRequest(false));
        entityManager.flush();
        entityManager.clear();

        assertThat(settingsService.retrieve(guest.getId(), room.getId()).enabled()).isFalse();
        assertThat(settingsService.retrieve(guest.getId(), otherRoom.getId()).enabled()).isTrue();
        assertThat(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(room.getId()))
                .containsExactly(host.getId());
        assertThat(chatRoomMemberRepository.findUserIdsByRoomId(room.getId()))
                .containsExactlyInAnyOrder(host.getId(), guest.getId());
        ChatRoomMember stored = chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), guest.getId()).orElseThrow();
        assertThat(stored.getLastReadMessage().getId()).isEqualTo(message.getId());

        chatRoomMemberRepository.delete(stored);
        entityManager.flush();
        entityManager.persist(ChatRoomMember.create(room, guest, ChatMemberRole.GUEST));
        entityManager.flush();
        entityManager.clear();
        assertThat(settingsService.retrieve(guest.getId(), room.getId()).enabled()).isTrue();
        assertThat(chatRoomMemberRepository.findNotificationEnabledUserIdsByRoomId(room.getId()))
                .containsExactlyInAnyOrder(host.getId(), guest.getId());
    }

    @ParameterizedTest
    @EnumSource(value = ChatMessageType.class, names = {"TEXT", "IMAGE", "POLL"})
    @DisplayName("실제 저장된 방별 OFF 멤버를 제외하고 ON 멤버에게만 메시지 푸시를 보낸다")
    void sendsPushOnlyToEnabledMembers(ChatMessageType type) {
        User host = persistUser("host", 1);
        User enabled = persistUser("enabled", 2);
        User muted = persistUser("muted", 3);
        Post post = persistPost(host, persistDestination());
        Journey journey = entityManager.persist(Journey.create(post));
        ChatRoom room = entityManager.persist(ChatRoom.createGroupChat(journey));
        entityManager.persist(JourneyMember.createHost(journey, host, LocalDateTime.of(2026, 11, 4, 0, 0)));
        entityManager.persist(JourneyMember.createMember(journey, enabled, LocalDateTime.of(2026, 11, 4, 0, 0)));
        entityManager.persist(JourneyMember.createMember(journey, muted, LocalDateTime.of(2026, 11, 4, 0, 0)));
        entityManager.persist(ChatRoomMember.create(room, host, ChatMemberRole.HOST));
        entityManager.persist(ChatRoomMember.create(room, enabled, ChatMemberRole.GUEST));
        entityManager.persist(ChatRoomMember.create(room, muted, ChatMemberRole.GUEST));
        settingsService.update(muted.getId(), room.getId(),
                new ChatNotificationSettingsRequest(false));
        entityManager.flush();
        entityManager.clear();

        var messages = mock(ChatMessageRepository.class);
        var online = mock(ChatOnlineStatusService.class);
        var push = mock(ChatPushNotificationService.class);
        var users = mock(UserRepository.class);
        var profile = new Profile(host);
        ReflectionTestUtils.setField(profile, "nickname", "호스트");
        ReflectionTestUtils.setField(host, "profile", profile);
        ChatMessage message = ChatMessage.create(room, host, type, "메시지");
        when(messages.findByIdWithSenderAndRoom(100L)).thenReturn(Optional.of(message));
        when(online.getOnlineUserIds(room.getId())).thenReturn(Set.of());
        when(users.findEnabledUserIds(List.of(enabled.getId()))).thenReturn(List.of(enabled.getId()));
        when(push.canSendPush(room.getId())).thenReturn(true);
        var listener = new ChatMessagePushEventListener(
                messages, chatRoomMemberRepository, online, push, users);

        listener.handle(new ChatMessageCreatedEvent(room.getId(), 100L));

        verify(push).sendPush(List.of(enabled.getId()), journey.getTitle(), "호스트", "메시지",
                type, ChatRoomType.GROUP, room.getId());
        verify(push).canSendPush(room.getId());
        verifyNoMoreInteractions(push);
    }

    private User persistUser(String name, int suffix) {
        User user = User.builder()
                .email(name + suffix + "@example.com")
                .name(name)
                .oauthId("oauth-" + suffix)
                .oauthType(OauthType.KAKAO)
                .build();
        return entityManager.persist(user);
    }

    private Destination persistDestination() {
        return entityManager.persist(Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .build());
    }

    private Post persistPost(User host, Destination destination) {
        Post post = Post.createPost(
                host,
                destination,
                "제주 여행",
                "채팅방 멤버 repository 테스트용 본문입니다.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                4,
                LocalDate.now().plusDays(1),
                Gender.U,
                true,
                null,
                null,
                "https://example.com/photo.png",
                "[]",
                null
        );
        return entityManager.persist(post);
    }
}
