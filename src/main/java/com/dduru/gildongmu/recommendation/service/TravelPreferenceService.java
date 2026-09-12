package com.dduru.gildongmu.recommendation.service;

import com.dduru.gildongmu.auth.exception.UserNotFoundException;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.exception.DestinationNotFoundException;
import com.dduru.gildongmu.recommendation.exception.DuplicateAvailableDateException;
import com.dduru.gildongmu.recommendation.exception.InvalidAvailableDateException;
import com.dduru.gildongmu.recommendation.exception.InvalidDestinationPreferenceException;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationAvailableDate;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.dto.request.AvailableDateRequest;
import com.dduru.gildongmu.recommendation.dto.request.DestinationPreferenceRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferencePatchRequest;
import com.dduru.gildongmu.recommendation.dto.response.AvailableDateResponse;
import com.dduru.gildongmu.recommendation.dto.response.DestinationPreferenceResponse;
import com.dduru.gildongmu.recommendation.dto.response.TravelPreferenceResponse;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationAvailableDateRepository;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TravelPreferenceService {

    private final UserRecommendationDestinationPreferenceRepository destinationPreferenceRepository;
    private final UserRecommendationAvailableDateRepository availableDateRepository;
    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;

    @Transactional(readOnly = true)
    public TravelPreferenceResponse getTravelPreferences(Long userId) {
        List<UserRecommendationDestinationPreference> destinationPreferences =
                destinationPreferenceRepository.findAllByUserIdWithDestination(userId);

        Set<String> countryCodes = destinationPreferences.stream()
                .filter(preference -> preference.getPreferenceType() == RecommendationDestinationPreferenceType.COUNTRY)
                .map(UserRecommendationDestinationPreference::getCountryCode)
                .collect(Collectors.toSet());

        Map<String, String> countryNameByCode = countryCodes.isEmpty() ? Map.of() :
                destinationRepository.findByCountryCodeIn(countryCodes).stream()
                        .collect(Collectors.toMap(Destination::getCountryCode, Destination::getCountryName, (existing, replacement) -> existing));

        List<DestinationPreferenceResponse> destinationPreferenceResponses = destinationPreferences.stream()
                .map(preference -> DestinationPreferenceResponse.from(preference, countryNameByCode))
                .toList();

        List<AvailableDateResponse> availableDateResponses = availableDateRepository.findAllByUser_Id(userId).stream()
                .map(AvailableDateResponse::from)
                .toList();

        return new TravelPreferenceResponse(destinationPreferenceResponses, availableDateResponses);
    }

    @Transactional
    public void updateTravelPreferences(Long userId, TravelPreferenceUpdateRequest request) {
        saveTravelPreferences(userId, request.destinationPreferences(), request.availableDates());
    }

    @Transactional
    public void patchTravelPreferences(Long userId, TravelPreferencePatchRequest request) {
        saveTravelPreferences(userId, request.getDestinationPreferences(), request.getAvailableDates());
    }

    private void saveTravelPreferences(Long userId, List<DestinationPreferenceRequest> destinations,
                                       List<AvailableDateRequest> dates) {
        if (destinations != null && destinations.size() > 3) {
            throw new InvalidDestinationPreferenceException();
        }
        if (dates != null) {
            validateAvailableDates(dates);
        }
        User user = userRepository.findByIdWithLock(userId).orElseThrow(UserNotFoundException::new);
        if (destinations != null) {
            replaceDestinationPreferences(user, destinations);
        }
        if (dates != null) {
            availableDateRepository.deleteAllByUserId(userId);
            availableDateRepository.saveAll(dates.stream()
                    .map(date -> UserRecommendationAvailableDate.of(user, date.startDate(), date.endDate()))
                    .toList());
        }
    }

    private void replaceDestinationPreferences(User user, List<DestinationPreferenceRequest> destinationPreferenceRequests) {
        List<Long> cityDestinationIds = destinationPreferenceRequests.stream()
                .filter(preferenceRequest -> preferenceRequest.type() == RecommendationDestinationPreferenceType.CITY)
                .map(DestinationPreferenceRequest::destinationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Destination> destinationById = destinationRepository.findAllById(cityDestinationIds).stream()
                .collect(Collectors.toMap(Destination::getId, destination -> destination));

        validateDestinationInputs(destinationPreferenceRequests, destinationById);

        List<DestinationPreferenceRequest> normalizedDestinationPreferences = deduplicateDestinationPreferences(destinationPreferenceRequests);

        destinationPreferenceRepository.deleteAllByUserId(user.getId());

        destinationPreferenceRepository.saveAll(
                IntStream.range(0, normalizedDestinationPreferences.size())
                        .mapToObj(index -> toDestinationPreferenceEntity(normalizedDestinationPreferences.get(index), user, destinationById, index + 1))
                        .toList()
        );
    }

    private void validateAvailableDates(List<AvailableDateRequest> availableDateRequests) {
        for (AvailableDateRequest availableDateRequest : availableDateRequests) {
            if (availableDateRequest.startDate().isAfter(availableDateRequest.endDate())) {
                throw new InvalidAvailableDateException();
            }
        }
        long uniqueDateRangeCount = availableDateRequests.stream()
                .distinct()
                .count();
        if (uniqueDateRangeCount < availableDateRequests.size()) {
            throw new DuplicateAvailableDateException();
        }
    }

    private void validateDestinationInputs(List<DestinationPreferenceRequest> destinationPreferenceRequests, Map<Long, Destination> destinationById) {
        List<DestinationPreferenceRequest> countryPreferenceRequests = destinationPreferenceRequests.stream()
                .filter(preferenceRequest -> preferenceRequest.type() == RecommendationDestinationPreferenceType.COUNTRY)
                .toList();

        validateCountryPreferences(countryPreferenceRequests);
        validateCityPreferences(destinationPreferenceRequests, destinationById);
    }

    private void validateCountryPreferences(List<DestinationPreferenceRequest> countryPreferenceRequests) {
        boolean hasBlankCountryCode = countryPreferenceRequests.stream()
                .anyMatch(preferenceRequest -> !StringUtils.hasText(preferenceRequest.countryCode()));

        if (hasBlankCountryCode) {
            throw new InvalidDestinationPreferenceException();
        }

        Set<String> requestedCountryCodes = countryPreferenceRequests.stream()
                .map(DestinationPreferenceRequest::countryCode)
                .collect(Collectors.toSet());

        if (!requestedCountryCodes.isEmpty()) {
            Set<String> existingCountryCodes = destinationRepository.findByCountryCodeIn(requestedCountryCodes).stream()
                    .map(Destination::getCountryCode)
                    .collect(Collectors.toSet());
            if (!existingCountryCodes.containsAll(requestedCountryCodes)) {
                throw new InvalidDestinationPreferenceException();
            }
        }
    }

    private void validateCityPreferences(
            List<DestinationPreferenceRequest> destinationPreferenceRequests,
            Map<Long, Destination> destinationById
    ) {
        for (DestinationPreferenceRequest preferenceRequest : destinationPreferenceRequests) {
            if (preferenceRequest.type() == RecommendationDestinationPreferenceType.CITY) {
                if (!destinationById.containsKey(preferenceRequest.destinationId())) {
                    throw new DestinationNotFoundException();
                }
            }
        }
    }

    // 동일한 COUNTRY 또는 CITY 중복 요청만 제거 (COUNTRY + 같은 국가 CITY 공존 허용)
    private List<DestinationPreferenceRequest> deduplicateDestinationPreferences(
            List<DestinationPreferenceRequest> destinationPreferenceRequests
    ) {
        Set<String> seenCountryCodes = new LinkedHashSet<>();
        Set<Long> seenDestinationIds = new LinkedHashSet<>();
        List<DestinationPreferenceRequest> effectivePreferenceRequests = new ArrayList<>();

        for (DestinationPreferenceRequest preferenceRequest : destinationPreferenceRequests) {
            if (preferenceRequest.type() == RecommendationDestinationPreferenceType.COUNTRY) {
                if (seenCountryCodes.add(preferenceRequest.countryCode())) {
                    effectivePreferenceRequests.add(preferenceRequest);
                }
            } else {
                if (seenDestinationIds.add(preferenceRequest.destinationId())) {
                    effectivePreferenceRequests.add(preferenceRequest);
                }
            }
        }
        return effectivePreferenceRequests;
    }

    private UserRecommendationDestinationPreference toDestinationPreferenceEntity(
            DestinationPreferenceRequest preferenceRequest,
            User user,
            Map<Long, Destination> destinationById,
            int preferenceRank
    ) {
        return switch (preferenceRequest.type()) {
            case COUNTRY -> UserRecommendationDestinationPreference.country(user, preferenceRequest.countryCode(), preferenceRank);
            case CITY -> UserRecommendationDestinationPreference.city(user, destinationById.get(preferenceRequest.destinationId()), preferenceRank);
        };
    }
}
