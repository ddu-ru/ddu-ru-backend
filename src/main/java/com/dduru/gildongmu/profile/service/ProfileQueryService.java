package com.dduru.gildongmu.profile.service;

import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.dto.response.MyProfileResponse;
import com.dduru.gildongmu.profile.dto.response.TravelStyleSummaryResponse;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final ProfileImageResolver profileImageResolver;
    private final TravelPreferenceService travelPreferenceService;
    private final JsonConverter jsonConverter;

    public MyProfileResponse getMyProfile(Long userId) {
        Profile profile = profileRepository.getByUserIdOrThrow(userId);
        TravelPreferenceResponse travelPreferences = travelPreferenceService.getTravelPreferences(userId);
        TravelStyleSummaryResponse travelStyleSummary =
                TravelStyleSummaryResponse.from(profile.getAvatar(), jsonConverter);
        return MyProfileResponse.of(
                profile,
                profileImageResolver,
                travelPreferences,
                travelStyleSummary
        );
    }
}
