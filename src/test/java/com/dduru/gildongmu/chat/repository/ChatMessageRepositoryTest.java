package com.dduru.gildongmu.chat.repository;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@DisplayName("ChatMessageRepository 테스트")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("room, beforeMessageId, visibleFrom 조건을 적용하고 id DESC로 제한 조회한다")
    void findVisibleMessagesAppliesCursorAndVisibleFrom() {
        User host = persistUser("host", 1);
        User guest = persistUser("guest", 2);
        Destination destination = persistDestination();
        Post post = persistPost(host, destination);
        ChatRoom room = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));

        ChatMessage oldMessage = persistMessage(room, host, "old");
        ChatMessage visible1 = persistMessage(room, guest, "visible1");
        ChatMessage visible2 = persistMessage(room, host, "visible2");
        ChatMessage cursor = persistMessage(room, guest, "cursor");
        ChatMessage otherRoomMessage = persistMessage(otherRoom, guest, "other");

        entityManager.flush();
        updateCreatedAt(oldMessage, LocalDateTime.of(2026, 5, 9, 8, 59));
        updateCreatedAt(visible1, LocalDateTime.of(2026, 5, 9, 9, 10));
        updateCreatedAt(visible2, LocalDateTime.of(2026, 5, 9, 9, 20));
        updateCreatedAt(cursor, LocalDateTime.of(2026, 5, 9, 9, 30));
        updateCreatedAt(otherRoomMessage, LocalDateTime.of(2026, 5, 9, 9, 40));
        entityManager.clear();

        List<ChatMessage> messages = chatMessageRepository.findVisibleMessages(
                room.getId(),
                cursor.getId(),
                LocalDateTime.of(2026, 5, 9, 9, 0),
                PageRequest.of(0, 2)
        );

        assertThat(messages).extracting(ChatMessage::getId)
                .containsExactly(visible2.getId(), visible1.getId());
    }

    @Test
    @DisplayName("findByIdAndRoomId는 지정한 방에 속한 메시지만 조회한다")
    void findByIdAndRoomIdReturnsOnlyMessageInRoom() {
        User host = persistUser("host", 3);
        User guest = persistUser("guest", 4);
        Destination destination = persistDestination();
        Post post = persistPost(host, destination);
        ChatRoom room = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatMessage message = persistMessage(room, host, "target");
        ChatMessage otherRoomMessage = persistMessage(otherRoom, guest, "other");
        entityManager.flush();
        entityManager.clear();

        assertThat(chatMessageRepository.findByIdAndRoomId(message.getId(), room.getId()))
                .isPresent()
                .get()
                .extracting(ChatMessage::getId)
                .isEqualTo(message.getId());
        assertThat(chatMessageRepository.findByIdAndRoomId(otherRoomMessage.getId(), room.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("커서 메시지 존재 여부는 방과 visibleFrom 조건을 함께 적용한다")
    void existsByIdAndRoomAndCreatedAtGreaterThanEqualAppliesVisibleFrom() {
        User host = persistUser("host", 1);
        Destination destination = persistDestination();
        Post post = persistPost(host, destination);
        ChatRoom room = entityManager.persist(ChatRoom.forPrivateChat(post));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));

        ChatMessage oldMessage = persistMessage(room, host, "old");
        ChatMessage visibleMessage = persistMessage(room, host, "visible");
        ChatMessage otherRoomMessage = persistMessage(otherRoom, host, "other");

        entityManager.flush();
        updateCreatedAt(oldMessage, LocalDateTime.of(2026, 5, 9, 8, 59));
        updateCreatedAt(visibleMessage, LocalDateTime.of(2026, 5, 9, 9, 0));
        updateCreatedAt(otherRoomMessage, LocalDateTime.of(2026, 5, 9, 9, 10));
        entityManager.clear();

        LocalDateTime visibleFrom = LocalDateTime.of(2026, 5, 9, 9, 0);

        assertThat(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                visibleMessage.getId(),
                room.getId(),
                visibleFrom
        )).isTrue();
        assertThat(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                oldMessage.getId(),
                room.getId(),
                visibleFrom
        )).isFalse();
        assertThat(chatMessageRepository.existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(
                otherRoomMessage.getId(),
                room.getId(),
                visibleFrom
        )).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ChatMessageType.class, names = {"IMAGE", "POLL"})
    @DisplayName("모아보기는 방·메시지 유형·참여 시각·커서를 적용하고 최신순으로 제한 조회한다")
    void findVisibleMessagesByTypeAppliesAllFilters(ChatMessageType type) {
        User host = persistUser("host", 1);
        Post post = persistPost(host, persistDestination());
        Journey journey = entityManager.persist(Journey.create(post));
        ChatRoom room = entityManager.persist(ChatRoom.createGroupChat(journey));
        ChatRoom otherRoom = entityManager.persist(ChatRoom.forPrivateChat(post));
        LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 1, 10, 0);

        ChatMessage extra = persistContentMessage(room, host, type, journey, joinedAt);
        ChatMessage boundary = persistContentMessage(room, host, type, journey, joinedAt);
        ChatMessage newest = persistContentMessage(room, host, type, journey, joinedAt);
        ChatMessage tooOld = persistContentMessage(room, host, type, journey, joinedAt);
        ChatMessage other = persistContentMessage(otherRoom, host, type, journey, joinedAt);
        ChatMessage text = persistMessage(room, host, "not content");
        ChatMessage cursor = persistContentMessage(room, host, type, journey, joinedAt);
        ChatMessage afterCursor = persistContentMessage(room, host, type, journey, joinedAt);
        entityManager.flush();
        for (ChatMessage message : List.of(extra, boundary, newest, other, text, cursor, afterCursor)) {
            updateCreatedAt(message, joinedAt);
        }
        updateCreatedAt(tooOld, joinedAt.minusSeconds(1));
        entityManager.clear();

        List<ChatMessage> messages = chatMessageRepository.findVisibleMessagesByType(
                room.getId(), type, cursor.getId(), joinedAt, PageRequest.of(0, 2));

        assertThat(messages).extracting(ChatMessage::getId).containsExactly(newest.getId(), boundary.getId());
        assertThat(messages).allMatch(message -> message.getMessageType() == type);
        if (type == ChatMessageType.POLL) {
            assertThat(entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
                    .isLoaded(messages.get(0), "poll")).isTrue();
        }
        assertThat(chatMessageRepository.findVisibleMessagesByType(
                room.getId(), type, null, joinedAt, PageRequest.of(0, 1)))
                .extracting(ChatMessage::getId).containsExactly(afterCursor.getId());
    }

    private ChatMessage persistContentMessage(
            ChatRoom room, User sender, ChatMessageType type, Journey journey, LocalDateTime now
    ) {
        if (type == ChatMessageType.POLL) {
            JourneyPoll poll = entityManager.persist(JourneyPoll.create(
                    journey, sender, "Dinner", false, false, false, now.plusDays(1), now));
            return entityManager.persist(ChatMessage.forPoll(room, poll));
        }
        return entityManager.persist(ChatMessage.create(room, sender, type, "https://example.com/photo.png"));
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
                "채팅 메시지 repository 테스트용 본문입니다.",
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

    private ChatMessage persistMessage(ChatRoom room, User sender, String content) {
        return entityManager.persist(ChatMessage.create(room, sender, ChatMessageType.TEXT, content));
    }

    private void updateCreatedAt(ChatMessage message, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE chat_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, message.getId())
                .executeUpdate();
    }
}
