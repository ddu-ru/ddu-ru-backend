package com.dduru.gildongmu.home.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.annotation.OptionalCurrentUser;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.dto.response.HomeResponse;
import com.dduru.gildongmu.home.dto.response.HomeSuperHostResponse;
import com.dduru.gildongmu.home.dto.response.MateRecommendationResponse;
import com.dduru.gildongmu.home.dto.response.SameAgeTripResponse;
import com.dduru.gildongmu.home.dto.response.SameDestinationTripResponse;
import com.dduru.gildongmu.home.dto.response.UpcomingTripResponse;
import com.dduru.gildongmu.home.service.HomeOverviewQueryService;
import com.dduru.gildongmu.home.service.HomePopularDestinationQueryService;
import com.dduru.gildongmu.home.service.HomeRecommendationQueryService;
import com.dduru.gildongmu.home.service.HomeSuperHostQueryService;
import com.dduru.gildongmu.home.service.HomeTripQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HomeController implements HomeApiDocs {

    private final HomeOverviewQueryService homeOverviewQueryService;
    private final HomeTripQueryService homeTripQueryService;
    private final HomePopularDestinationQueryService homePopularDestinationQueryService;
    private final HomeRecommendationQueryService homeRecommendationQueryService;
    private final HomeSuperHostQueryService homeSuperHostQueryService;

    @Override
    @GetMapping(HomeEndpoints.HOME)
    public ResponseEntity<ApiResult<HomeResponse>> retrieveHome(@OptionalCurrentUser Long userId) {
        HomeResponse response = homeOverviewQueryService.retrieve(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.UPCOMING_TRIP)
    public ResponseEntity<ApiResult<UpcomingTripResponse>> retrieveUpcomingTrip(
            @CurrentUser Long userId
    ) {
        UpcomingTripResponse response = homeTripQueryService.retrieveUpcomingTrip(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.POPULAR_DESTINATIONS)
    public ResponseEntity<ApiResult<HomePopularDestinationResponse>> retrievePopularDestinations() {
        HomePopularDestinationResponse response = homePopularDestinationQueryService.retrieve();
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.MATE_RECOMMENDATIONS)
    public ResponseEntity<ApiResult<MateRecommendationResponse>> retrieveMateRecommendations(
            @CurrentUser Long userId
    ) {
        MateRecommendationResponse response = homeRecommendationQueryService.retrieve(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.SUPER_HOSTS)
    public ResponseEntity<ApiResult<List<HomeSuperHostResponse>>> retrieveSuperHosts(
            @OptionalCurrentUser Long userId
    ) {
        List<HomeSuperHostResponse> response = homeSuperHostQueryService.retrieve(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.SAME_DESTINATION_TRIPS)
    public ResponseEntity<ApiResult<List<SameDestinationTripResponse>>> retrieveSameDestinationTrips(
            @CurrentUser Long userId
    ) {
        List<SameDestinationTripResponse> response = homeTripQueryService.retrieveSameDestinationTrips(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping(HomeEndpoints.SAME_AGE_TRIPS)
    public ResponseEntity<ApiResult<List<SameAgeTripResponse>>> retrieveSameAgeTrips(
            @CurrentUser Long userId
    ) {
        List<SameAgeTripResponse> response = homeTripQueryService.retrieveSameAgeTrips(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
