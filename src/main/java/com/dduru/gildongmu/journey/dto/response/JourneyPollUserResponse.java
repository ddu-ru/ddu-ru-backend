package com.dduru.gildongmu.journey.dto.response;

import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.user.domain.User;
import org.springframework.util.StringUtils;

public record JourneyPollUserResponse(Long userId, String nickname, String profileImageUrl) {
    public static JourneyPollUserResponse from(User user, ProfileImageResolver resolver) {
        String nickname = user.getProfile() == null ? null : user.getProfile().getNickname();
        return new JourneyPollUserResponse(user.getId(), StringUtils.hasText(nickname) ? nickname : user.getName(),
                resolver.resolve(user.getProfile()));
    }
}
