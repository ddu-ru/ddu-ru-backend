package com.dduru.gildongmu.home.cache;

import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomePopularDestinationCache {

    static final String KEY = "home:popular-destinations:v1";
    static final Duration TTL = Duration.ofHours(1);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<HomePopularDestinationResponse> get() {
        try {
            String json = redisTemplate.opsForValue().get(KEY);
            if (json == null) return Optional.empty();
            var response = objectMapper.readValue(json, HomePopularDestinationResponse.class);
            if (!isValidResponse(response)) {
                log.warn("홈 인기 여행지 캐시 형식 오류; DB 집계로 전환");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (DataAccessException | JsonProcessingException e) {
            log.warn("홈 인기 여행지 캐시 조회 실패; DB 집계로 전환", e);
            return Optional.empty();
        }
    }

    public void put(HomePopularDestinationResponse response) {
        try {
            redisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(response), TTL);
        } catch (DataAccessException | JsonProcessingException e) {
            log.warn("홈 인기 여행지 캐시 저장 실패; 집계 결과 반환", e);
        }
    }

    private boolean isValidResponse(HomePopularDestinationResponse response) {
        return response != null
                && response.updatedAt() != null
                && response.items() != null
                && response.items().size() <= 5
                && response.items().stream().allMatch(this::isValidItem);
    }

    private boolean isValidItem(HomePopularDestinationResponse.Item item) {
        return item != null
                && item.destinationId() != null
                && item.source() != null
                && item.tags() != null;
    }
}
