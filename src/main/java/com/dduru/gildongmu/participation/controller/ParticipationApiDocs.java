package com.dduru.gildongmu.participation.controller;

import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.participation.dto.request.ParticipationRequest;
import com.dduru.gildongmu.participation.dto.request.ParticipationRetrieveRequest;
import com.dduru.gildongmu.participation.dto.response.MyParticipationResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationApproveResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationContactResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationCreateResponse;
import com.dduru.gildongmu.participation.dto.response.ParticipationRetrieveResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Participations", description = "동행 게시글 참여 관리 API")
@SecurityRequirement(name = "JWT")
public interface ParticipationApiDocs {

    @Operation(
            summary = "참여 신청",
            description = "게시글에 참여 신청을 합니다. 모집이 마감되었거나 정원이 가득 찬 게시글에는 신청할 수 없습니다. 동일 사용자의 중복 신청은 오류(409)를 반환합니다."
    )
    @ApiResponse(responseCode = "201", description = "참여 신청 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.RECRUITMENT_CLOSED,
            ErrorCode.RECRUITMENT_FULL,
            ErrorCode.DUPLICATE_PARTICIPATION,
            ErrorCode.SELF_PARTICIPATION_NOT_ALLOWED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<ParticipationCreateResponse>> createParticipation(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "게시글 ID") Long postId,
            @Valid ParticipationRequest request
    );

    @Operation(
            summary = "참여 신청 연락 시작",
            description = "방장이 신청자와 1:1 채팅을 시작합니다. `PENDING`인 경우에만 `CONTACTING`으로 바뀌며, 응답의 `privateRoomId`로 연락 채팅방을 열 수 있습니다. 이미 연락 중·승인·거절된 신청이면 오류를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "연락 시작 성공")
    @ApiErrorResponses({
            ErrorCode.PARTICIPATION_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.RECRUITMENT_CLOSED,
            ErrorCode.RECRUITMENT_FULL,
            ErrorCode.PARTICIPATION_CONTACT_NOT_ALLOWED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<ParticipationContactResponse>> contactParticipation(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "동행 참여 신청 ID") Long participationId
    );

    @Operation(
            summary = "참여 신청 승인",
            description = "방장이 신청자를 승인합니다. `PENDING` 또는 `CONTACTING` 상태에서만 승인할 수 있으며, 그룹 단톡에 초대한 뒤 `APPROVED`로 바뀝니다. 응답의 `groupRoomId`는 항상 포함됩니다. 이미 승인·거절된 신청이면 오류를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "참여 신청 승인 성공")
    @ApiErrorResponses({
            ErrorCode.PARTICIPATION_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.RECRUITMENT_CLOSED,
            ErrorCode.RECRUITMENT_FULL,
            ErrorCode.PARTICIPATION_APPROVAL_NOT_ALLOWED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.CHAT_ROOM_NOT_FOUND,
            ErrorCode.CHAT_ROOM_CLOSED,
            ErrorCode.CHAT_ROOM_CAPACITY_EXCEEDED,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<ParticipationApproveResponse>> approveParticipation(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "동행 참여 신청 ID") Long participationId
    );

    @Operation(
            summary = "참여 신청 거절",
            description = "게시글 참여 신청을 거절합니다. 요청 본문은 없으며, 이미 승인·거절된 신청에 대해 다시 호출하면 오류를 반환합니다."
    )
    @ApiResponse(responseCode = "204", description = "참여 신청 거절 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.POST_NOT_FOUND,
            ErrorCode.PARTICIPATION_NOT_FOUND,
            ErrorCode.POST_ACCESS_DENIED,
            ErrorCode.RECRUITMENT_CLOSED,
            ErrorCode.RECRUITMENT_FULL,
            ErrorCode.PARTICIPATION_REJECTION_NOT_ALLOWED,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<Void>> rejectParticipation(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "동행 참여 신청 ID") Long participationId
    );

    @Operation(summary = "내가 받은 참여 신청 목록 조회", description = "내가 작성한 전체 게시글 기준으로 받은 동행 신청 목록을 최신순으로 조회합니다. `status` query parameter로 상태 필터링이 가능합니다.")
    @ApiResponse(responseCode = "200", description = "신청자 목록 조회 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<ParticipationRetrieveResponse>>> getParticipants(
            @Parameter(hidden = true) Long userId,
            @Valid @ParameterObject ParticipationRetrieveRequest request
    );

    @Operation(
            summary = "내가 보낸 동행 신청 목록(신청 내역)",
            description = "로그인 사용자가 신청한 동행 참여 건을 상태 구분 없이 최신순 전체로 조회합니다. 연락 중·수락 건에는 채팅방 이동에 필요한 방 ID가 포함될 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "신청 내역 조회 성공")
    @ApiErrorResponses({
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<MyParticipationResponse>>> getMyParticipationApplications(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "내 동행 신청 취소",
            description = "신청자 본인이 `PENDING`(대기중)인 참여 신청만 삭제(취소)합니다. 연락 중 이후 상태에서는 호출할 수 없습니다."
    )
    @ApiResponse(responseCode = "204", description = "신청 취소 성공", content = @Content())
    @ApiErrorResponses({
            ErrorCode.PARTICIPATION_NOT_FOUND,
            ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<Void>> cancelMyParticipation(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "동행 참여 신청 ID") Long participationId
    );
}
