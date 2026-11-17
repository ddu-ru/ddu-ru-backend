package com.dduru.gildongmu.profile.service;

import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.dto.response.MyProfileResponse;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final ProfileImageResolver profileImageResolver;

    public MyProfileResponse getMyProfile(Long userId) {
        Profile profile = profileRepository.getByUserIdOrThrow(userId);
        return MyProfileResponse.of(profile, profileImageResolver);
    }
}
