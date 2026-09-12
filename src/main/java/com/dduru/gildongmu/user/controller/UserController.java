package com.dduru.gildongmu.user.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.post.dto.request.MyPageLikedPostListRequest;
import com.dduru.gildongmu.post.dto.request.MyPagePostListRequest;
import com.dduru.gildongmu.post.dto.response.MyPageLikedPostListResponse;
import com.dduru.gildongmu.post.dto.response.MyPagePostListResponse;
import com.dduru.gildongmu.post.service.PostQueryService;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferencePatchRequest;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController implements UserApiDocs {

    private final PostQueryService postQueryService;
    private final TravelPreferenceService travelPreferenceService;

    @Override
    @GetMapping("/liked-posts")
    public ResponseEntity<ApiResult<MyPageLikedPostListResponse>> retrieveMyLikedPosts(
            @CurrentUser Long userId,
            @Valid MyPageLikedPostListRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(postQueryService.retrieveMyLikedPosts(userId, request)));
    }

    @Override
    @GetMapping("/posts")
    public ResponseEntity<ApiResult<MyPagePostListResponse>> retrieveMyPosts(
            @CurrentUser Long userId,
            @Valid MyPagePostListRequest request
    ) {
        MyPagePostListResponse response = postQueryService.retrieveMyPosts(userId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/travel-preferences")
    public ResponseEntity<ApiResult<TravelPreferenceResponse>> getTravelPreferences(
            @CurrentUser Long userId
    ) {
        return ResponseEntity.ok(ApiResult.ok(travelPreferenceService.getTravelPreferences(userId)));
    }

    @Override
    @PutMapping("/travel-preferences")
    public ResponseEntity<ApiResult<Void>> updateTravelPreferences(
            @CurrentUser Long userId,
            @Valid @RequestBody TravelPreferenceUpdateRequest request
    ) {
        travelPreferenceService.updateTravelPreferences(userId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.noContent());
    }

    @Override
    @PatchMapping("/travel-preferences")
    public ResponseEntity<ApiResult<Void>> patchTravelPreferences(
            @CurrentUser Long userId,
            @Valid @RequestBody TravelPreferencePatchRequest request
    ) {
        travelPreferenceService.patchTravelPreferences(userId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.noContent());
    }

}
