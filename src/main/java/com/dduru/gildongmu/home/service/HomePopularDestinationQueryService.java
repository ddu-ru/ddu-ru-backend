package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.home.cache.HomePopularDestinationCache;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomePopularDestinationQueryService {

    private final HomePopularDestinationCache cache;
    private final HomePopularDestinationAggregationService aggregationService;

    public HomePopularDestinationResponse retrieve() {
        return cache.get().orElseGet(() -> {
            var response = aggregationService.aggregate();
            cache.put(response);
            return response;
        });
    }
}
