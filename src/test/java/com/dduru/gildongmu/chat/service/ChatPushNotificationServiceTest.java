package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.fcm.service.FcmPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatPushNotificationService 테스트")
class ChatPushNotificationServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private FcmPushService fcmPushService;

    private ChatPushNotificationService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new ChatPushNotificationService(redisTemplate, fcmPushService);
    }

    @Nested
    @DisplayName("canSendPush — Leading Edge Debounce")
    class CanSendPush {

        @Test
        @DisplayName("키가 없으면 SET NX 성공 → true 반환 (발송 가능)")
        void returnsTrueWhenKeyAbsent() {
            when(valueOps.setIfAbsent(eq("chat:push:lastsent:5"), eq("1"), eq(Duration.ofSeconds(30))))
                    .thenReturn(true);

            assertThat(service.canSendPush(5L)).isTrue();
        }

        @Test
        @DisplayName("키가 이미 있으면 SET NX 실패 → false 반환 (쿨다운 중)")
        void returnsFalseWhenKeyExists() {
            when(valueOps.setIfAbsent(eq("chat:push:lastsent:5"), eq("1"), eq(Duration.ofSeconds(30))))
                    .thenReturn(false);

            assertThat(service.canSendPush(5L)).isFalse();
        }
    }

    @Nested
    @DisplayName("sendPush — FCM 발송")
    class SendPush {

        @Test
        void sendsPollTitleAsPreview() {
            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", "저녁 메뉴", ChatMessageType.POLL, ChatRoomType.GROUP, 5L);
            verify(fcmPushService).sendToUsers(eq(List.of(1L)), eq("도쿄 여행"),
                    eq("홍길동: 투표가 등록되었습니다: 저녁 메뉴"), eq("chat:5"), any());
        }

        @Test
        @DisplayName("TEXT 메시지는 닉네임: 내용 형식으로 발송한다")
        void sendsTextMessageWithNicknameAndContent() {
            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", "오늘 저녁 어때?", ChatMessageType.TEXT, ChatRoomType.GROUP, 5L);

            verify(fcmPushService).sendToUsers(
                    eq(List.of(1L)), eq("도쿄 여행"), eq("홍길동: 오늘 저녁 어때?"), eq("chat:5"), any()
            );
        }

        @Test
        @DisplayName("GROUP TEXT가 30자를 초과하면 30자 + ...으로 잘린다")
        void truncatesTextContentExceeding30Chars() {
            String longContent = "가".repeat(35);

            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", longContent, ChatMessageType.TEXT, ChatRoomType.GROUP, 5L);

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fcmPushService).sendToUsers(any(), any(), bodyCaptor.capture(), any(), any());

            assertThat(bodyCaptor.getValue()).isEqualTo("홍길동: " + "가".repeat(30) + "...");
        }

        @Test
        @DisplayName("GROUP TEXT가 30자 이하면 그대로 발송한다")
        void doesNotTruncateContentWithin30Chars() {
            String shortContent = "가".repeat(30);

            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", shortContent, ChatMessageType.TEXT, ChatRoomType.GROUP, 5L);

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fcmPushService).sendToUsers(any(), any(), bodyCaptor.capture(), any(), any());

            assertThat(bodyCaptor.getValue()).isEqualTo("홍길동: " + shortContent);
        }

        @Test
        @DisplayName("GROUP IMAGE는 '닉네임: 사진을 보냈습니다.' 형식으로 발송한다")
        void sendsGroupImageMessage() {
            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", "https://s3.example.com/img.jpg",
                    ChatMessageType.IMAGE, ChatRoomType.GROUP, 5L);

            verify(fcmPushService).sendToUsers(any(), any(), eq("홍길동: 사진을 보냈습니다."), any(), any());
        }

        @Test
        @DisplayName("PRIVATE TEXT는 닉네임 없이 내용만 body에 담아 발송한다")
        void sendsPrivateTextWithoutNickname() {
            service.sendPush(List.of(1L), "홍길동", "홍길동", "안녕하세요", ChatMessageType.TEXT, ChatRoomType.PRIVATE, 5L);

            verify(fcmPushService).sendToUsers(any(), any(), eq("안녕하세요"), any(), any());
        }

        @Test
        @DisplayName("PRIVATE IMAGE는 닉네임 없이 '사진을 보냈습니다.'로 발송한다")
        void sendsPrivateImageWithoutNickname() {
            service.sendPush(List.of(1L), "홍길동", "홍길동", "https://s3.example.com/img.jpg",
                    ChatMessageType.IMAGE, ChatRoomType.PRIVATE, 5L);

            verify(fcmPushService).sendToUsers(any(), any(), eq("사진을 보냈습니다."), any(), any());
        }

        @Test
        @DisplayName("collapseKey는 chat:{roomId} 형식으로 전달된다")
        void sendsWithCorrectCollapseKey() {
            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", "안녕", ChatMessageType.TEXT, ChatRoomType.GROUP, 5L);

            verify(fcmPushService).sendToUsers(any(), any(), any(), eq("chat:5"), any());
        }

        @Test
        @DisplayName("FCM data payload에 CHAT_ROOM resourceType과 roomId가 포함된다")
        void sendsWithChatRoomDataPayload() {
            service.sendPush(List.of(1L), "도쿄 여행", "홍길동", "안녕", ChatMessageType.TEXT, ChatRoomType.GROUP, 5L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(fcmPushService).sendToUsers(any(), any(), any(), any(), dataCaptor.capture());

            assertThat(dataCaptor.getValue()).containsEntry("resourceType", "CHAT_ROOM");
            assertThat(dataCaptor.getValue()).containsEntry("resourceId", "5");
        }
    }
}
