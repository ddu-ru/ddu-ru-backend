package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse.Source;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository.DestinationCount;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HomePopularDestinationAggregationServiceTest {
    @Test
    void reusesRankedCountsAndDefaultsFallbackCountToZero() {
        var time = mock(TimeProvider.class);
        var queries = mock(HomePopularDestinationRepository.class);
        var destinations = mock(DestinationRepository.class);
        var json = mock(JsonConverter.class);
        var first = destination(2L, "제주");
        var second = destination(1L, "부산");
        var fallback = destination(3L, "강릉");
        when(time.now()).thenReturn(LocalDateTime.of(2026, 10, 11, 12, 0));
        when(queries.findRankedDestinationCounts(5))
                .thenReturn(List.of(new DestinationCount(2L, 25), new DestinationCount(1L, 20)));
        when(destinations.findAllById(List.of(2L, 1L))).thenReturn(List.of(second, first));
        when(queries.findFallbackDestinations(List.of(2L, 1L), 3)).thenReturn(List.of(fallback));
        when(queries.findOpenCounts(List.of(2L, 1L, 3L)))
                .thenReturn(List.of(new DestinationCount(1L, 4)));
        var result = new HomePopularDestinationAggregationService(time, queries, destinations, json).aggregate();
        assertThat(result.items()).extracting("destinationId").containsExactly(2L, 1L, 3L);
        assertThat(result.items()).extracting("totalTripCount").containsExactly(25L, 20L, 0L);
        assertThat(result.items()).extracting("source").containsExactly(Source.POPULAR, Source.POPULAR, Source.DEFAULT);
        assertThat(result.items().get(1).availableTripCount()).isEqualTo(4);
        verify(queries).findTags(List.of(2L, 1L));
    }

    @Test
    void reusesAllRankedCountsWithoutFallback() {
        TimeProvider time = mock(TimeProvider.class);
        HomePopularDestinationRepository queries = mock(HomePopularDestinationRepository.class);
        DestinationRepository destinations = mock(DestinationRepository.class);
        JsonConverter json = mock(JsonConverter.class);
        List<Long> rankedIds = List.of(1L, 2L, 3L, 4L, 5L);
        when(queries.findRankedDestinationCounts(5)).thenReturn(rankedIds.stream()
                .map(id -> new DestinationCount(id, 10L)).toList());
        List<Destination> rankedDestinations = rankedIds.stream()
                .map(id -> destination(id, "여행지" + id)).toList();
        when(destinations.findAllById(rankedIds)).thenReturn(rankedDestinations);

        var result = new HomePopularDestinationAggregationService(time, queries, destinations, json).aggregate();

        assertThat(result.items()).extracting("totalTripCount").containsExactly(10L, 10L, 10L, 10L, 10L);
        verify(queries, never()).findFallbackDestinations(anyCollection(), anyInt());
    }

    private Destination destination(Long id, String name) {
        var destination = mock(Destination.class);
        when(destination.getId()).thenReturn(id);
        when(destination.getCity()).thenReturn(name);
        return destination;
    }
}
