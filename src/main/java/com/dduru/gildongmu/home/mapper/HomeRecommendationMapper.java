package com.dduru.gildongmu.home.mapper;

import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.home.dto.response.HomeHostResponse;
import com.dduru.gildongmu.home.dto.response.MateRecommendationResponse;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.profile.dto.response.ProfileImageInfo;
import com.dduru.gildongmu.profile.utils.AgeCalculator;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.recommendation.dto.query.MateRecommendationCardQueryResult;
import com.dduru.gildongmu.recommendation.dto.result.RecommendationReason;
import com.dduru.gildongmu.recommendation.support.RecommendationReasonJsonConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class HomeRecommendationMapper {

    private final RecommendationReasonJsonConverter recommendationReasonJsonConverter;
    private final ProfileImageResolver profileImageResolver;
    private final JsonConverter jsonConverter;

    public MateRecommendationResponse.Item toResponse(MateRecommendationCardQueryResult card, LocalDate today) {
        return new MateRecommendationResponse.Item(
                card.recommendationId(),
                card.postId(),
                card.matchPercentage(),
                card.title(),
                card.thumbnailUrl(),
                card.countryName() + " " + card.city(),
                card.startDate(),
                card.endDate(),
                toHost(card, today),
                card.recruitCount(),
                card.recruitCapacity(),
                card.content(),
                jsonConverter.convertJsonToList(card.tags()),
                recommendationReasonJsonConverter.fromJson(card.matchReasons()).stream()
                        .map(HomeRecommendationMapper::toReason)
                        .toList(),
                recommendationReasonJsonConverter.fromJson(card.cautionPoints()).stream()
                        .map(HomeRecommendationMapper::toReason)
                        .toList()
        );
    }

    private HomeHostResponse toHost(MateRecommendationCardQueryResult card, LocalDate today) {
        ProfileImageType imageType = card.hostProfileImageType() == null
                ? ProfileImageType.DEFAULT
                : card.hostProfileImageType();
        return new HomeHostResponse(
                card.hostNickname(),
                ProfileImageInfo.from(
                        imageType,
                        card.hostUploadedImageUrl(),
                        card.hostAvatarImageUrl(),
                        card.hostBgColorId(),
                        profileImageResolver
                ),
                AgeCalculator.calculate(card.hostBirthday(), today),
                card.hostGender()
        );
    }

    private static MateRecommendationResponse.Reason toReason(RecommendationReason reason) {
        return new MateRecommendationResponse.Reason(reason.code(), reason.message());
    }
}
