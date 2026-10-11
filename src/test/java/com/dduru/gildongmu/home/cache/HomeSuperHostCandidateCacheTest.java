package com.dduru.gildongmu.home.cache;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.home.repository.HomeSuperHostQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(HomeSuperHostCandidateCacheTest.TestConfig.class)
@DisplayName("홈 슈퍼호스트 후보 캐시 테스트")
class HomeSuperHostCandidateCacheTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired
    private HomeSuperHostCandidateCache candidateCache;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private HomeSuperHostQueryRepository queryRepository;

    @MockitoBean
    private TimeProvider timeProvider;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(HomeSuperHostCandidateCache.CACHE_NAME).clear();
    }

    @Test
    @DisplayName("후보 ID를 캐시해 반복 조회에서 DB를 다시 호출하지 않는다")
    void cachesCandidatePostIds() {
        when(timeProvider.now()).thenReturn(NOW);
        when(queryRepository.findVisibleCandidatePostIds(NOW, NOW.toLocalDate()))
                .thenReturn(List.of(11L, 12L, 13L));

        assertThat(candidateCache.retrieve().values()).containsExactly(11L, 12L, 13L);
        assertThat(candidateCache.retrieve().values()).containsExactly(11L, 12L, 13L);

        verify(queryRepository, times(1)).findVisibleCandidatePostIds(NOW, NOW.toLocalDate());
    }

    @Test
    @DisplayName("후보 ID 캐시 값은 Redis 직렬화 후에도 Long 목록으로 복원된다")
    void serializesCandidatePostIds() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        var source = new HomeSuperHostCandidateCache.CandidatePostIds(List.of(11L, 12L));

        Object restored = serializer.deserialize(serializer.serialize(source));

        assertThat(restored).isEqualTo(source);
        assertThat(((HomeSuperHostCandidateCache.CandidatePostIds) restored).values())
                .allMatch(value -> value instanceof Long);
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(HomeSuperHostCandidateCache.CACHE_NAME);
        }

        @Bean
        HomeSuperHostCandidateCache candidateCache(
                HomeSuperHostQueryRepository queryRepository,
                TimeProvider timeProvider
        ) {
            return new HomeSuperHostCandidateCache(queryRepository, timeProvider);
        }
    }
}
