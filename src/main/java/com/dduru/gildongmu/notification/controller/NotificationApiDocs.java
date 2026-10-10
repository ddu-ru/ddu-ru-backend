package com.dduru.gildongmu.notification.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.notification.dto.request.NotificationListRequest;
import com.dduru.gildongmu.notification.dto.request.NotificationSettingsRequest;
import com.dduru.gildongmu.notification.dto.response.NotificationListResponse;
import com.dduru.gildongmu.notification.dto.response.NotificationReadResponse;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
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

@Tag(name = "Notifications", description = "알림 API")
@SecurityRequirement(name = "JWT")
public interface NotificationApiDocs {

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    최근 한 달(조회 시각에서 달력 기준 한 달 전부터)의 삭제되지 않은 본인 알림을 ID 내림차순으로 조회합니다.
                    filter: ALL(전체, 기본값), UNREAD(읽지 않음), MATCH(모집·매칭), JOURNEY(여정).
                    MATCH에는 매칭 신청·승인·찜한 글 변경, JOURNEY에는 공지·일정 생성/변경/취소·여행 임박이 포함됩니다.
                    채팅 메시지는 푸시만 발송하며 목록에 포함하지 않습니다. cursor 미전달 시 첫 페이지를 조회합니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE})
    ResponseEntity<ApiResult<NotificationListResponse>> getNotifications(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject NotificationListRequest request
    );

    @Operation(summary = "안 읽은 알림 개수 조회", description = "최근 한 달의 삭제되지 않은 미읽음 알림을 전체·모집/매칭·여정별로 집계합니다. 탭이나 페이지와 무관한 개수입니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<UnreadCountResponse>> getUnreadCount(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "단건 읽음 처리", description = "최근 한 달의 삭제되지 않은 본인 알림 1건을 읽음 처리합니다. 이미 읽은 알림은 멱등 처리하며, 삭제되거나 조회 기간이 지난 알림은 NOT_FOUND입니다.")
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.NOTIFICATION_NOT_FOUND,
            ErrorCode.NOTIFICATION_ACCESS_DENIED
    })
    ResponseEntity<ApiResult<NotificationReadResponse>> markAsRead(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId
    );

    @Operation(summary = "전체 읽음 처리", description = "선택한 탭과 관계없이 최근 한 달의 삭제되지 않은 본인 미읽음 알림을 일괄 읽음 처리합니다.")
    @ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<NotificationReadResponse>> markAllAsRead(
            @Parameter(hidden = true) Long userId
    );

    @Operation(summary = "전체 알림 삭제", description = "선택한 탭과 관계없이 최근 한 달의 본인 알림을 일괄 소프트 삭제합니다. 이미 삭제되었거나 알림이 없어도 성공합니다. 푸시 발송 이력은 유지됩니다.")
    @ApiResponse(responseCode = "204", description = "전체 삭제 성공 (응답 본문 없음)")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED})
    ResponseEntity<ApiResult<Void>> deleteAllNotifications(@Parameter(hidden = true) Long userId);

    @Operation(summary = "알림 설정 변경", description = "알림 수신 전체 on/off를 설정합니다.")
    @ApiResponse(responseCode = "204", description = "설정 변경 성공")
    @ApiErrorResponses({ErrorCode.UNAUTHORIZED, ErrorCode.INVALID_INPUT_VALUE})
    ResponseEntity<ApiResult<Void>> updateNotificationSettings(
            @Parameter(hidden = true) Long userId,
            @Valid @RequestBody NotificationSettingsRequest request
    );
}
