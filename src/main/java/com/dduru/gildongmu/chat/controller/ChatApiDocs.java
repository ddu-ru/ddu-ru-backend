package com.dduru.gildongmu.chat.controller;

import com.dduru.gildongmu.chat.dto.request.ChatMessageRetrieveRequest;
import com.dduru.gildongmu.chat.dto.request.ChatReadRequest;
import com.dduru.gildongmu.chat.dto.request.ChatRoomListRequest;
import com.dduru.gildongmu.chat.dto.response.ChatMessagesResponse;
import com.dduru.gildongmu.chat.dto.response.ChatReadResponse;
import com.dduru.gildongmu.chat.dto.response.ChatRoomListResponse;
import com.dduru.gildongmu.chat.dto.response.PrivateChatRoomCreateResponse;
import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Chat", description = "채팅 API")
@SecurityRequirement(name = "JWT")
public interface ChatApiDocs {

    @Operation(
            summary = "1:1 채팅방 생성/조회",
            description = "게시글 단위로 현재 사용자와 게시글 작성자 사이의 1:1 채팅방을 생성합니다. " +
                    "이미 존재하면 기존 방을 재사용하며 isCreated=false와 함께 HTTP 200을 반환하고, 새로 생성되면 isCreated=true와 함께 HTTP 201을 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "기존 채팅방 조회")
    @ApiResponse(responseCode = "201", description = "신규 채팅방 생성")
    @ApiErrorResponses({
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.NOT_SELF_CHAT
    })
    ResponseEntity<ApiResult<PrivateChatRoomCreateResponse>> createPrivateRoom(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId
    );

    @Operation(
            summary = "채팅방 메시지 목록 조회",
            description = "채팅방의 메시지를 beforeMessageId 커서 기준으로 조회합니다. " +
                    "첫 페이지는 beforeMessageId 없이 호출하고, 다음 과거 페이지는 page.nextCursor를 beforeMessageId로 전달합니다. " +
                    "응답 메시지는 오래된 순서로 반환되며 messageType에 따라 content(TEXT), images(IMAGE), systemMessage(SYSTEM), poll(POLL) 중 하나를 사용합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.CHAT_ROOM_NOT_FOUND,
            ErrorCode.CHAT_ACCESS_DENIED,
            ErrorCode.CHAT_MESSAGE_NOT_FOUND,
            ErrorCode.JSON_CONVERT_ERROR
    })
    ResponseEntity<ApiResult<ChatMessagesResponse>> retrieveMessages(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "채팅방 ID", required = true) Long chatRoomId,
            @Valid @ParameterObject ChatMessageRetrieveRequest request
    );

    @Operation(
            summary = "채팅방 메시지 읽음 처리",
            description = "현재 사용자의 채팅방 읽음 위치를 lastReadMessageId까지 갱신합니다. " +
                    "읽음 위치가 실제로 전진한 경우 updated=true이며, 기존 읽음 위치와 같거나 과거 메시지를 요청한 경우 updated=false로 응답합니다."
    )
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.CHAT_ROOM_NOT_FOUND,
            ErrorCode.CHAT_MESSAGE_NOT_FOUND,
            ErrorCode.CHAT_ACCESS_DENIED
    })
    ResponseEntity<ApiResult<ChatReadResponse>> readMessages(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "채팅방 ID", required = true) Long chatRoomId,
            @Valid @RequestBody ChatReadRequest request
    );

    @Operation(
            summary = "채팅방 목록 조회",
            description = "현재 사용자가 참여 중인 ACTIVE 채팅방 목록을 마지막 활동 시각 기준으로 조회합니다. " +
                    "roomType은 ALL, PRIVATE, GROUP을 지원하며 생략 시 ALL입니다. " +
                    "다음 페이지 조회 시에는 이전 응답의 page.nextCursor를 cursor로 그대로 전달합니다. " +
                    "목록 표시는 displayName, thumbnailUrl, lastMessage, unreadCount, activityAt 값을 우선 사용합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.JSON_CONVERT_ERROR
    })
    ResponseEntity<ApiResult<ChatRoomListResponse>> retrieveChatRooms(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject ChatRoomListRequest request
    );
}
