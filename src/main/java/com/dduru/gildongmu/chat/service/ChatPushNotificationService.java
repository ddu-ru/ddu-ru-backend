package com.dduru.gildongmu.chat.service;

import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.chat.domain.enums.ChatRoomType;
import com.dduru.gildongmu.fcm.service.FcmPushService;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatPushNotificationService {

    private static final String COOLDOWN_KEY_PREFIX = "chat:push:lastsent:";
    private static final Duration COOLDOWN_DURATION = Duration.ofSeconds(30);
    private static final int CONTENT_MAX_LENGTH = 30;

    private final RedisTemplate<String, String> redisTemplate;
    private final FcmPushService fcmPushService;

    // SET NX EX 30 — 원자적 연산으로 leading edge debounce 구현
    // 키 없음(30초 경과 or 최초): SET 성공 → true (발송 가능, 쿨다운 시작)
    // 키 있음(30초 이내): SET 생략 → false (쿨다운 중, 스킵)
    public boolean canSendPush(Long roomId) {
        Boolean keySet = redisTemplate.opsForValue()
                .setIfAbsent(COOLDOWN_KEY_PREFIX + roomId, "1", COOLDOWN_DURATION);
        return Boolean.TRUE.equals(keySet); // Redis 오류 시 null 반환시 false로 처리 → 언박싱 NPE 방지
    }

    public void sendPush(List<Long> userIds, String roomTitle, String senderNickname,
                         String content, ChatMessageType messageType, ChatRoomType roomType, Long roomId) {
        String body = buildBody(senderNickname, content, messageType, roomType);
        fcmPushService.sendToUsers(userIds, roomTitle, body, "chat:" + roomId,
                FcmPushService.dataPayload(ResourceType.CHAT_ROOM, roomId));
    }

    private String buildBody(String senderNickname, String content, ChatMessageType messageType, ChatRoomType roomType) {
        if (roomType == ChatRoomType.PRIVATE) {
            return switch (messageType) {
                case TEXT -> truncate(content);
                case IMAGE -> "사진을 보냈습니다.";
                case POLL -> "투표가 등록되었습니다: " + truncate(content);
                default -> "메시지를 보냈습니다.";
            };
        }
        return switch (messageType) {
            case TEXT -> senderNickname + ": " + truncate(content);
            case IMAGE -> senderNickname + ": 사진을 보냈습니다.";
            case POLL -> senderNickname + ": 투표가 등록되었습니다: " + truncate(content);
            default -> senderNickname + ": 메시지를 보냈습니다.";
        };
    }

    // length() 대신 codePointCount 사용 — 이모지 등 유니코드 보조 문자가 length()에서 2로 카운트되는 문제 방지
    private String truncate(String content) {
        int codePointLength = content.codePointCount(0, content.length());
        if (codePointLength <= CONTENT_MAX_LENGTH) return content;
        int endIndex = content.offsetByCodePoints(0, CONTENT_MAX_LENGTH);
        return content.substring(0, endIndex) + "...";
    }
}
