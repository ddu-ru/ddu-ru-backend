package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.home.dto.query.HomeSuperHostQueryResult;
import com.dduru.gildongmu.home.dto.response.HomeHostResponse;
import com.dduru.gildongmu.home.dto.response.HomeSuperHostResponse;
import com.dduru.gildongmu.home.repository.HomeSuperHostQueryRepository;
import com.dduru.gildongmu.like.repository.PostLikeRepository;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.profile.dto.response.ProfileImageInfo;
import com.dduru.gildongmu.profile.utils.AgeCalculator;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeSuperHostQueryService {

    private final HomeSuperHostQueryRepository queryRepository;
    private final PostLikeRepository postLikeRepository;
    private final ProfileImageResolver profileImageResolver;
    private final JsonConverter jsonConverter;
    private final TimeProvider timeProvider;

    public List<HomeSuperHostResponse> retrieve(Long userId) {
        LocalDateTime now = timeProvider.now();
        LocalDate today = now.toLocalDate();
        List<HomeSuperHostQueryResult> superHosts = queryRepository.findVisibleSuperHosts(userId, now, today);
        if (superHosts.isEmpty()) {
            return List.of();
        }

        Set<Long> likedPostIds = findLikedPostIds(userId, superHosts);
        return superHosts.stream()
                .map(superHost -> toResponse(superHost, today, likedPostIds.contains(superHost.postId())))
                .toList();
    }

    private Set<Long> findLikedPostIds(Long userId, List<HomeSuperHostQueryResult> superHosts) {
        if (userId == null) {
            return Set.of();
        }
        List<Long> postIds = superHosts.stream().map(HomeSuperHostQueryResult::postId).toList();
        return postLikeRepository.findLikedPostIdsByUserId(userId, postIds);
    }

    private HomeSuperHostResponse toResponse(HomeSuperHostQueryResult superHost, LocalDate today, boolean hasLiked) {
        return new HomeSuperHostResponse(
                superHost.postId(),
                HomeSuperHostResponse.Status.OPEN,
                superHost.title(),
                superHost.countryName() + " " + superHost.city(),
                superHost.startDate(),
                superHost.endDate(),
                superHost.currentMemberCount(),
                superHost.maxMemberCount(),
                jsonConverter.convertJsonToList(superHost.tags()),
                toHost(superHost, today),
                superHost.viewCount(),
                superHost.thumbnailUrl(),
                hasLiked
        );
    }

    private HomeHostResponse toHost(HomeSuperHostQueryResult superHost, LocalDate today) {
        ProfileImageType imageType = superHost.hostProfileImageType() == null
                ? ProfileImageType.DEFAULT
                : superHost.hostProfileImageType();
        return new HomeHostResponse(
                superHost.hostNickname(),
                ProfileImageInfo.from(
                        imageType,
                        superHost.hostUploadedImageUrl(),
                        superHost.hostAvatarImageUrl(),
                        superHost.hostBgColorId(),
                        profileImageResolver
                ),
                AgeCalculator.calculate(superHost.hostBirthday(), today),
                superHost.hostGender()
        );
    }
}
