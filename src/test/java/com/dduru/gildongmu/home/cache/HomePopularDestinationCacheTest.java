package com.dduru.gildongmu.home.cache;

import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.service.HomePopularDestinationAggregationService;
import com.dduru.gildongmu.home.service.HomePopularDestinationQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomePopularDestinationCacheTest {
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final HomePopularDestinationCache cache = new HomePopularDestinationCache(redis, mapper);
    private final HomePopularDestinationResponse response = new HomePopularDestinationResponse(
            LocalDateTime.of(2026, 9, 19, 12, 0, 12), List.of(
            new HomePopularDestinationResponse.Item(1, 122L, "제주", null, 3, List.of("바다"), 10,
                    HomePopularDestinationResponse.Source.POPULAR)));

    @Test
    void roundTripsSnapshotWithOneHourTtl() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        cache.put(response);
        String json = mapper.writeValueAsString(response);
        verify(values).set(HomePopularDestinationCache.KEY, json, Duration.ofHours(1));
        when(values.get(HomePopularDestinationCache.KEY)).thenReturn(json);
        assertThat(cache.get()).contains(response);
    }

    @Test
    void missingAndCorruptEntriesBecomeCacheMisses() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(HomePopularDestinationCache.KEY)).thenReturn(null, "broken json", "{}", "null");
        for (int i = 0; i < 4; i++) assertThat(cache.get()).isEmpty();
    }

    @Test
    void redisReadAndWriteFailuresStillReturnDatabaseResult() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("offline"));
        doThrow(new RedisConnectionFailureException("offline")).when(values).set(anyString(), anyString(), any(Duration.class));
        var aggregation = mock(HomePopularDestinationAggregationService.class);
        when(aggregation.aggregate()).thenReturn(response);
        assertThat(new HomePopularDestinationQueryService(cache, aggregation).retrieve()).isEqualTo(response);
        verify(aggregation).aggregate();
    }
}
