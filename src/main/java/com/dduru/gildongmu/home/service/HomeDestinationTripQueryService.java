package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.home.dto.response.SameDestinationTripResponse;
import com.dduru.gildongmu.home.repository.HomeDestinationTripQueryRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDestinationTripQueryService {

    private final UserRecommendationDestinationPreferenceRepository preferenceRepository;
    private final HomeDestinationTripQueryRepository queryRepository;
    private final TimeProvider timeProvider;

    public List<SameDestinationTripResponse> retrieve(Long userId) {
        return preferenceRepository.findFirstPreferenceByUserId(userId)
                .map(preference -> queryRepository.findTrips(userId, timeProvider.today(), preference))
                .orElseGet(List::of);
    }
}
