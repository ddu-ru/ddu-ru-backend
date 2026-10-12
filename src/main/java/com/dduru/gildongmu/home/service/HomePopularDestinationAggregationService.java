package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse.Item;
import com.dduru.gildongmu.home.dto.response.HomePopularDestinationResponse.Source;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository.DestinationCount;
import com.dduru.gildongmu.home.repository.HomePopularDestinationRepository.DestinationTags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomePopularDestinationAggregationService {

    private static final int DESTINATION_LIMIT = 5;
    private static final int TAG_LIMIT = 3;
    private final TimeProvider timeProvider;
    private final HomePopularDestinationRepository queryRepository;
    private final DestinationRepository destinationRepository;
    private final JsonConverter jsonConverter;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HomePopularDestinationResponse aggregate() {
        LocalDateTime updatedAt = timeProvider.now();
        List<DestinationCount> rankedDestinationCounts = queryRepository.findRankedDestinationCounts(DESTINATION_LIMIT);
        List<Long> rankedDestinationIds = rankedDestinationCounts.stream()
                .map(DestinationCount::destinationId).toList();
        List<Destination> destinations = selectDestinations(rankedDestinationIds);
        List<Long> destinationIds = destinations.stream().map(Destination::getId).toList();

        Map<Long, Long> totalTripCounts = toCountMap(rankedDestinationCounts);
        Map<Long, Long> openTripCounts = toCountMap(queryRepository.findOpenCounts(destinationIds));
        Map<Long, List<String>> popularTagsByDestination = aggregatePopularTags(rankedDestinationIds);
        List<Item> items = createItems(destinations, Set.copyOf(rankedDestinationIds),
                totalTripCounts, openTripCounts, popularTagsByDestination);

        return new HomePopularDestinationResponse(updatedAt, items);
    }

    private List<Destination> selectDestinations(List<Long> rankedDestinationIds) {
        Map<Long, Destination> destinationsById = destinationRepository.findAllById(rankedDestinationIds).stream()
                .collect(Collectors.toMap(Destination::getId, destination -> destination));
        List<Destination> destinations = new ArrayList<>(rankedDestinationIds.stream()
                .map(destinationsById::get)
                .filter(Objects::nonNull)
                .toList());
        int remainingCount = DESTINATION_LIMIT - destinations.size();
        if (remainingCount > 0) {
            List<Long> selectedDestinationIds = destinations.stream().map(Destination::getId).toList();
            destinations.addAll(queryRepository.findFallbackDestinations(selectedDestinationIds, remainingCount));
        }

        return destinations;
    }

    private Map<Long, Long> toCountMap(List<DestinationCount> counts) {
        return counts.stream()
                .collect(Collectors.toMap(DestinationCount::destinationId, DestinationCount::count));
    }

    private Map<Long, List<String>> aggregatePopularTags(List<Long> rankedDestinationIds) {
        Map<Long, Map<String, Long>> tagCountsByDestination = new HashMap<>();
        for (DestinationTags destinationTags : queryRepository.findTags(rankedDestinationIds)) {
            Map<String, Long> tagCounts = tagCountsByDestination.computeIfAbsent(
                    destinationTags.destinationId(), ignored -> new HashMap<>());
            jsonConverter.convertJsonToList(destinationTags.tags()).stream()
                    .filter(Objects::nonNull)
                    .map(String::strip)
                    .filter(tag -> !tag.isEmpty())
                    .distinct()
                    .forEach(tag -> tagCounts.merge(tag, 1L, Long::sum));
        }

        return tagCountsByDestination.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> selectTopTags(entry.getValue())));
    }

    private List<String> selectTopTags(Map<String, Long> tagCounts) {
        return tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TAG_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Item> createItems(
            List<Destination> destinations,
            Set<Long> rankedDestinationIds,
            Map<Long, Long> totalTripCounts,
            Map<Long, Long> openTripCounts,
            Map<Long, List<String>> popularTagsByDestination) {
        List<Item> items = new ArrayList<>();
        for (Destination destination : destinations) {
            Long destinationId = destination.getId();
            Source source = rankedDestinationIds.contains(destinationId) ? Source.POPULAR : Source.DEFAULT;
            items.add(new Item(
                    items.size() + 1, destinationId, destination.getCity(), destination.getImage(),
                    openTripCounts.getOrDefault(destinationId, 0L),
                    popularTagsByDestination.getOrDefault(destinationId, List.of()),
                    totalTripCounts.getOrDefault(destinationId, 0L), source));
        }
        return List.copyOf(items);
    }
}
