package com.dduru.gildongmu.recommendation.service;

import com.dduru.gildongmu.common.config.QueryDslConfig;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.dto.request.DestinationPreferenceRequest;
import com.dduru.gildongmu.recommendation.dto.request.AvailableDateRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationAvailableDateRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, TravelPreferenceService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TravelPreferenceConcurrencyTest {
    @Autowired UserRepository users;
    @Autowired DestinationRepository destinations;
    @Autowired UserRecommendationDestinationPreferenceRepository preferences;
    @Autowired UserRecommendationAvailableDateRepository dates;
    @Autowired TravelPreferenceService service;

    @Test
    void concurrentReplacementKeepsOneCompleteOrderedList() throws Exception {
        String key = UUID.randomUUID().toString();
        User user = users.save(User.builder().email(key + "@example.com").name("동시수정")
                .oauthId(key).oauthType(OauthType.KAKAO).build());
        Destination city = destinations.save(Destination.builder().countryCode("JP").countryName("일본")
                .city("rank-concurrency-" + key).image("https://example.com/image.jpg").build());
        var cityRequest = new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.CITY, null, city.getId());
        var countryRequest = new DestinationPreferenceRequest(RecommendationDestinationPreferenceType.COUNTRY, "JP", null);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            var tasks = List.of(List.of(cityRequest, countryRequest), List.of(countryRequest, cityRequest));
            var futures = tasks.stream().map(items -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                for (int i = 0; i < 5; i++) {
                    service.updateTravelPreferences(user.getId(), testRequest(items, List.of()));
                }
                return null;
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
            var result = service.getTravelPreferences(user.getId()).destinationPreferences();
            assertThat(result).extracting("preferenceRank").containsExactly(1, 2);
            assertThat(result).extracting("type").containsExactlyInAnyOrder(
                    RecommendationDestinationPreferenceType.CITY, RecommendationDestinationPreferenceType.COUNTRY);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            preferences.deleteAll(preferences.findAllByUserIdWithDestination(user.getId()));
            dates.deleteAll(dates.findAllByUser_Id(user.getId()));
            users.deleteById(user.getId());
            destinations.deleteById(city.getId());
        }
    }
    private TravelPreferenceUpdateRequest testRequest(
            List<DestinationPreferenceRequest> destinationPreferences,
            List<AvailableDateRequest> availableDates
    ) {
        return new TravelPreferenceUpdateRequest(destinationPreferences, availableDates);
    }

}
