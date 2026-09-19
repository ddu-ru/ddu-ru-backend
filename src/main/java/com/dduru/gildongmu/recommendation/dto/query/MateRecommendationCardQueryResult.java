package com.dduru.gildongmu.recommendation.dto.query;

import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;

import java.time.LocalDate;

public record MateRecommendationCardQueryResult(
        Long recommendationId,
        Long postId,
        int recommendationRank,
        int matchPercentage,
        String matchReasons,
        String cautionPoints,
        String title,
        String thumbnailUrl,
        String countryName,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        CompanionType companionType,
        int recruitCount,
        int recruitCapacity,
        String content,
        String tags,
        String hostNickname,
        ProfileImageType hostProfileImageType,
        String hostUploadedImageUrl,
        String hostAvatarImageUrl,
        Long hostBgColorId,
        LocalDate hostBirthday,
        Gender hostGender
) {
}
