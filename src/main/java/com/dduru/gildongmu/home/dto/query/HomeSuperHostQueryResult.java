package com.dduru.gildongmu.home.dto.query;

import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDate;

public record HomeSuperHostQueryResult(
        Long postId,
        String title,
        String countryName,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        Integer currentMemberCount,
        Integer maxMemberCount,
        String tags,
        Integer viewCount,
        String thumbnailUrl,
        String hostNickname,
        ProfileImageType hostProfileImageType,
        String hostUploadedImageUrl,
        String hostAvatarImageUrl,
        Long hostBgColorId,
        LocalDate hostBirthday,
        Gender hostGender
) {

    @QueryProjection
    public HomeSuperHostQueryResult {
    }
}
