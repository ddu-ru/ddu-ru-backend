package com.dduru.gildongmu.superhost.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.superhost.dto.response.MySuperHostStatusResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostApplyResponse;
import com.dduru.gildongmu.superhost.service.SuperHostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class SuperHostController implements SuperHostApiDocs {
    private final SuperHostService superHostService;

    @Override
    @GetMapping("/super-hosts/me")
    public ResponseEntity<ApiResult<MySuperHostStatusResponse>> retrieveMySuperHostStatus(
            @CurrentUser Long userId
    ) {
        MySuperHostStatusResponse response = superHostService.getMyStatus(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PostMapping("/{postId}/super-host")
    public ResponseEntity<ApiResult<SuperHostApplyResponse>> applySuperHostTicket(
            @PathVariable Long postId,
            @CurrentUser Long userId
    ) {
        SuperHostApplyResponse response = superHostService.applyTicketToPost(userId, postId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
