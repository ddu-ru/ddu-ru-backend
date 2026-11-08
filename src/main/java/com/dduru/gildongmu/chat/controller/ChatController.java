package com.dduru.gildongmu.chat.controller;

import com.dduru.gildongmu.chat.dto.request.ChatMessageRetrieveRequest;
import com.dduru.gildongmu.chat.dto.request.ChatContentListRequest;
import com.dduru.gildongmu.chat.dto.response.ChatImageListResponse;
import com.dduru.gildongmu.chat.dto.response.ChatPollListResponse;
import com.dduru.gildongmu.chat.dto.request.ChatNotificationSettingsRequest;
import com.dduru.gildongmu.chat.dto.request.ChatReadRequest;
import com.dduru.gildongmu.chat.dto.request.ChatRoomListRequest;
import com.dduru.gildongmu.chat.dto.response.ChatMessagesResponse;
import com.dduru.gildongmu.chat.dto.response.ChatNotificationSettingsResponse;
import com.dduru.gildongmu.chat.dto.response.ChatReadResponse;
import com.dduru.gildongmu.chat.dto.response.ChatRoomListResponse;
import com.dduru.gildongmu.chat.dto.response.PrivateChatRoomCreateResponse;
import com.dduru.gildongmu.chat.service.ChatMessageQueryService;
import com.dduru.gildongmu.chat.service.ChatNotificationSettingsService;
import com.dduru.gildongmu.chat.service.ChatReadService;
import com.dduru.gildongmu.chat.service.ChatRoomListService;
import com.dduru.gildongmu.chat.service.PrivateChatRoomService;
import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChatController implements ChatApiDocs {

    private final PrivateChatRoomService privateChatRoomService;
    private final ChatMessageQueryService chatMessageQueryService;
    private final ChatReadService chatReadService;
    private final ChatRoomListService chatRoomListService;
    private final ChatNotificationSettingsService chatNotificationSettingsService;

    @Override
    @PostMapping("/posts/{postId}/chats/private")
    public ResponseEntity<ApiResult<PrivateChatRoomCreateResponse>> createPrivateRoom(
            @CurrentUser Long userId,
            @PathVariable Long postId
    ) {
        PrivateChatRoomCreateResponse response = privateChatRoomService.createOrGetRoom(userId, postId);
        if (!response.isCreated()) {
            return ResponseEntity.ok(ApiResult.ok(response));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }

    @Override
    @GetMapping("/chat-rooms/{chatRoomId}/messages")
    public ResponseEntity<ApiResult<ChatMessagesResponse>> retrieveMessages(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId,
            @Valid @ModelAttribute ChatMessageRetrieveRequest request
    ) {
        ChatMessagesResponse response = chatMessageQueryService.retrieveMessages(userId, chatRoomId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PatchMapping("/chat-rooms/{chatRoomId}/read")
    public ResponseEntity<ApiResult<ChatReadResponse>> readMessages(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatReadRequest request
    ) {
        ChatReadResponse response = chatReadService.read(userId, chatRoomId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/chat-rooms/{chatRoomId}/images")
    public ResponseEntity<ApiResult<ChatImageListResponse>> retrieveImages(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId,
            @Valid @ModelAttribute ChatContentListRequest request
    ) {
        ChatImageListResponse response = chatMessageQueryService.retrieveImages(userId, chatRoomId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/chat-rooms/{chatRoomId}/polls")
    public ResponseEntity<ApiResult<ChatPollListResponse>> retrievePolls(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId,
            @Valid @ModelAttribute ChatContentListRequest request
    ) {
        ChatPollListResponse response = chatMessageQueryService.retrievePolls(userId, chatRoomId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/chat-rooms")
    public ResponseEntity<ApiResult<ChatRoomListResponse>> retrieveChatRooms(
            @CurrentUser Long userId,
            @Valid @ModelAttribute ChatRoomListRequest request
    ) {
        ChatRoomListResponse response = chatRoomListService.retrieveChatRooms(userId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/chat-rooms/{chatRoomId}/notification-settings")
    public ResponseEntity<ApiResult<ChatNotificationSettingsResponse>> retrieveNotificationSettings(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId
    ) {
        return ResponseEntity.ok(ApiResult.ok(chatNotificationSettingsService.retrieve(userId, chatRoomId)));
    }

    @Override
    @PatchMapping("/chat-rooms/{chatRoomId}/notification-settings")
    public ResponseEntity<ApiResult<ChatNotificationSettingsResponse>> updateNotificationSettings(
            @CurrentUser Long userId,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatNotificationSettingsRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(chatNotificationSettingsService.update(userId, chatRoomId, request)));
    }
}
