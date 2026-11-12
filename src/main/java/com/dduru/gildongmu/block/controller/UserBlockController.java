package com.dduru.gildongmu.block.controller;

import com.dduru.gildongmu.block.dto.request.BlockedUserListRequest;
import com.dduru.gildongmu.block.dto.response.BlockedUserListResponse;
import com.dduru.gildongmu.block.service.UserBlockService;
import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/blocks")
public class UserBlockController implements UserBlockApiDocs {

    private final UserBlockService userBlockService;

    @Override
    @PutMapping("/{targetUserId}")
    public ResponseEntity<ApiResult<Void>> block(@CurrentUser Long userId, @PathVariable Long targetUserId) {
        userBlockService.block(userId, targetUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.noContent());
    }

    @Override
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResult<Void>> unblock(@CurrentUser Long userId, @PathVariable Long targetUserId) {
        userBlockService.unblock(userId, targetUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.noContent());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<BlockedUserListResponse>> getBlockedUsers(
            @CurrentUser Long userId,
            @Valid BlockedUserListRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(userBlockService.getBlockedUsers(userId, request)));
    }
}
