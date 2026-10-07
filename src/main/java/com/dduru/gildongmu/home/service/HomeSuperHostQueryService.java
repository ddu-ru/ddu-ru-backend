package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.home.cache.HomeSuperHostCandidateCache;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeSuperHostQueryService {

    private static final int HOME_SUPER_HOST_LIMIT = 5;
    private static final int CANDIDATE_BATCH_SIZE = 10;

    private final HomeSuperHostCandidateCache candidateCache;
    private final HomeSuperHostQueryRepository queryRepository;
    private final PostLikeRepository postLikeRepository;
    private final ProfileImageResolver profileImageResolver;
    private final JsonConverter jsonConverter;
    private final TimeProvider timeProvider;

    public List<HomeSuperHostResponse> retrieve(Long userId) {
        LocalDateTime now = timeProvider.now();
        LocalDate today = now.toLocalDate();
        List<Long> candidatePostIds = new ArrayList<>(candidateCache.retrieve().values());
        Collections.shuffle(candidatePostIds);

        List<HomeSuperHostQueryResult> superHosts = selectVisibleSuperHosts(
                userId, now, today, candidatePostIds
        );
        if (superHosts.isEmpty()) {
            return List.of();
        }

        Set<Long> likedPostIds = findLikedPostIds(userId, superHosts);
        return superHosts.stream()
                .map(superHost -> toResponse(superHost, today, likedPostIds.contains(superHost.postId())))
                .toList();
    }

    private List<HomeSuperHostQueryResult> selectVisibleSuperHosts(
            Long userId,
            LocalDateTime now,
            LocalDate today,
            List<Long> candidatePostIds
    ) {
        List<HomeSuperHostQueryResult> selected = new ArrayList<>(HOME_SUPER_HOST_LIMIT);
        for (int fromIndex = 0;
             fromIndex < candidatePostIds.size() && selected.size() < HOME_SUPER_HOST_LIMIT;
             fromIndex += CANDIDATE_BATCH_SIZE) {
            int toIndex = Math.min(fromIndex + CANDIDATE_BATCH_SIZE, candidatePostIds.size());
            List<Long> batchIds = candidatePostIds.subList(fromIndex, toIndex);
            Map<Long, HomeSuperHostQueryResult> visibleByPostId = queryRepository
                    .findVisibleSuperHosts(userId, now, today, batchIds)
                    .stream()
                    .collect(HashMap::new,
                            (map, result) -> map.putIfAbsent(result.postId(), result),
                            Map::putAll);

            for (Long postId : batchIds) {
                HomeSuperHostQueryResult visible = visibleByPostId.get(postId);
                if (visible != null) {
                    selected.add(visible);
                }
                if (selected.size() == HOME_SUPER_HOST_LIMIT) {
                    break;
                }
            }
        }
        return selected;
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
