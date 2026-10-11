package com.dduru.gildongmu.home.cache;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.home.repository.HomeSuperHostQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeSuperHostCandidateCache {

    public static final String CACHE_NAME = "homeSuperHostCandidates";

    private final HomeSuperHostQueryRepository queryRepository;
    private final TimeProvider timeProvider;

    @Cacheable(cacheNames = CACHE_NAME, key = "'all'", sync = true)
    public CandidatePostIds retrieve() {
        LocalDateTime now = timeProvider.now();
        return new CandidatePostIds(queryRepository.findVisibleCandidatePostIds(now, now.toLocalDate()));
    }

    public record CandidatePostIds(List<Long> values) {
        public CandidatePostIds {
            values = List.copyOf(values);
        }
    }
}
