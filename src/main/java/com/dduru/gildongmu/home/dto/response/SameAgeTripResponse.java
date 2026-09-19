package com.dduru.gildongmu.home.dto.response;

import java.time.LocalDate;

public record SameAgeTripResponse(
        Long postId,
        String title,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        int currentMemberCount,
        int maxMemberCount,
        String thumbnailUrl
) {
}
