package com.dduru.gildongmu.home.dto.response;

import com.dduru.gildongmu.home.enums.UserAccessStatus;

import java.util.List;

public record HomeResponse(
        UserAccessStatus userAccessStatus,
        List<HomeSectionResponse> sections
) {

    public record HomeSectionResponse(
            SectionKey key,
            boolean enabled,
            String endpoint,
            DisabledReason disabledReason
    ) {
    }

    public enum SectionKey {
        UPCOMING_TRIP,
        POPULAR_DESTINATIONS,
        MATE_RECOMMENDATIONS,
        SUPER_HOSTS,
        SAME_DESTINATION_TRIPS,
        SAME_AGE_TRIPS
    }

    public enum DisabledReason {
        LOGIN_REQUIRED,
        SURVEY_REQUIRED,
        DESTINATION_PREFERENCE_REQUIRED
    }
}
