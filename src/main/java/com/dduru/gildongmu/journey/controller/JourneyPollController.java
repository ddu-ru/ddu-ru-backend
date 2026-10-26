package com.dduru.gildongmu.journey.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.journey.dto.request.JourneyPollCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollListRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollOptionCreateRequest;
import com.dduru.gildongmu.journey.dto.request.JourneyPollVoteRequest;
import com.dduru.gildongmu.journey.dto.response.JourneyPollListResponse;
import com.dduru.gildongmu.journey.dto.response.JourneyPollResponse;
import com.dduru.gildongmu.journey.service.JourneyPollQueryService;
import com.dduru.gildongmu.journey.service.JourneyPollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/journeys/{journeyId}/polls")
public class JourneyPollController implements JourneyPollApiDocs {
    private final JourneyPollService journeyPollService;
    private final JourneyPollQueryService journeyPollQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResult<JourneyPollListResponse>> retrievePolls(
            @PathVariable Long journeyId,
            @CurrentUser Long userId,
            @Valid @ModelAttribute JourneyPollListRequest request
    ) {
        JourneyPollListResponse response = journeyPollQueryService.retrievePolls(journeyId, userId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResult<JourneyPollResponse>> retrievePoll(
            @PathVariable Long journeyId,
            @PathVariable Long pollId,
            @CurrentUser Long userId
    ) {
        JourneyPollResponse response = journeyPollQueryService.retrievePoll(journeyId, pollId, userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResult<JourneyPollResponse>> createPoll(
            @PathVariable Long journeyId,
            @CurrentUser Long userId,
            @Valid @RequestBody JourneyPollCreateRequest request
    ) {
        JourneyPollResponse response = journeyPollService.createPoll(journeyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }

    @Override
    @PutMapping("/{pollId}/votes/me")
    public ResponseEntity<ApiResult<JourneyPollResponse>> vote(
            @PathVariable Long journeyId,
            @PathVariable Long pollId,
            @CurrentUser Long userId,
            @Valid @RequestBody JourneyPollVoteRequest request
    ) {
        JourneyPollResponse response = journeyPollService.vote(journeyId, pollId, userId, request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PostMapping("/{pollId}/options")
    public ResponseEntity<ApiResult<JourneyPollResponse>> addOption(
            @PathVariable Long journeyId,
            @PathVariable Long pollId,
            @CurrentUser Long userId,
            @Valid @RequestBody JourneyPollOptionCreateRequest request
    ) {
        JourneyPollResponse response = journeyPollService.addOption(journeyId, pollId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(response));
    }
}
