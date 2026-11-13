package com.dduru.gildongmu.block.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "차단 목록 조회 응답")
public record BlockedUserListResponse(
        List<BlockedUserResponse> users,
        Long nextCursor,
        boolean hasNext
) {
    public static BlockedUserListResponse of(List<BlockedUserResponse> users, boolean hasNext, Long nextCursor) {
        return new BlockedUserListResponse(users, nextCursor, hasNext);
    }
}
