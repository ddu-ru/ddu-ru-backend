package com.dduru.gildongmu.user.service;

import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import com.dduru.gildongmu.user.dto.response.MyPageOverviewResponse;
import com.dduru.gildongmu.user.dto.response.MyPageTravelStyleSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageOverviewQueryService {

    private final ProfileRepository profileRepository;
    private final ProfileImageResolver profileImageResolver;
    private final TravelPreferenceService travelPreferenceService;
    private final JsonConverter jsonConverter;

    public MyPageOverviewResponse retrieve(Long userId) {
        Profile profile = profileRepository.getByUserIdOrThrow(userId);
        TravelPreferenceResponse travelPreferences = travelPreferenceService.getTravelPreferences(userId);
        MyPageTravelStyleSummaryResponse travelStyleSummary =
                MyPageTravelStyleSummaryResponse.from(profile.getAvatar(), jsonConverter);
        return MyPageOverviewResponse.of(
                profile,
                profileImageResolver,
                travelPreferences,
                travelStyleSummary
        );
    }
}
