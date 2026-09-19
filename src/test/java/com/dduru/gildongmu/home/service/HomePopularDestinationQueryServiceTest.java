package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.home.cache.HomePopularDestinationCache;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomePopularDestinationQueryServiceTest {
    private final HomePopularDestinationCache cache = mock(HomePopularDestinationCache.class);
    private final HomePopularDestinationAggregationService aggregation = mock(HomePopularDestinationAggregationService.class);
    private final HomePopularDestinationQueryService service = new HomePopularDestinationQueryService(cache, aggregation);
    private final HomePopularDestinationResponse response = new HomePopularDestinationResponse(
            LocalDateTime.of(2026, 9, 19, 12, 0), List.of());

    @Test
    void cacheHitPreservesSnapshotAndDoesNotQueryDatabase() {
        when(cache.get()).thenReturn(Optional.of(response));
        assertThat(service.retrieve()).isSameAs(response);
        verifyNoInteractions(aggregation);
        verify(cache, never()).put(any());
    }

    @Test
    void expirationReaggregatesAndStoresNewSnapshot() {
        var next = new HomePopularDestinationResponse(response.updatedAt().plusHours(1), List.of());
        when(cache.get()).thenReturn(Optional.of(response), Optional.empty());
        when(aggregation.aggregate()).thenReturn(next);
        assertThat(service.retrieve().updatedAt()).isEqualTo(response.updatedAt());
        assertThat(service.retrieve().updatedAt()).isEqualTo(next.updatedAt());
        verify(aggregation).aggregate();
        verify(cache).put(next);
    }

    @Test
    void databaseFailureIsNotDisguisedAsEmptyOrDefaultResults() {
        when(cache.get()).thenReturn(Optional.empty());
        when(aggregation.aggregate()).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(service::retrieve).isInstanceOf(IllegalStateException.class);
        verify(cache, never()).put(any());
    }
}
