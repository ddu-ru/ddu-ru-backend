package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.home.dto.response.SameAgeTripResponse;
import com.dduru.gildongmu.home.dto.response.SameDestinationTripResponse;
import com.dduru.gildongmu.home.dto.response.UpcomingTripResponse;
import com.dduru.gildongmu.home.repository.HomeTripQueryRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.exception.CurrentOrUpcomingJourneyNotFoundException;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.journey.repository.JourneyScheduleRepository;
import com.dduru.gildongmu.profile.exception.BirthdayNotFoundException;
import com.dduru.gildongmu.profile.repository.ProfileRepository;
import com.dduru.gildongmu.profile.utils.AgeCalculator;
import com.dduru.gildongmu.recommendation.repository.UserRecommendationDestinationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeTripQueryService {

    private static final int HOME_TRIP_DISPLAY_LIMIT = 3;

    private final TimeProvider timeProvider;
    private final ProfileRepository profileRepository;
    private final JourneyScheduleRepository journeyScheduleRepository;
    private final JourneyRepository journeyRepository;
    private final UserRecommendationDestinationPreferenceRepository preferenceRepository;
    private final HomeTripQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public UpcomingTripResponse retrieveUpcomingTrip(Long userId) {
        LocalDate today = timeProvider.today();
        Journey journey = journeyRepository
                .findNearestCurrentOrUpcomingJourney(userId, today)
                .orElseThrow(CurrentOrUpcomingJourneyNotFoundException::new);

        int scheduleCount = journeyScheduleRepository.countByJourneyIdAndIsDeletedFalse(journey.getId());
        return UpcomingTripResponse.from(journey, today, scheduleCount);
    }

    @Transactional(readOnly = true)
    public List<SameDestinationTripResponse> retrieveSameDestinationTrips(Long userId) {
        return preferenceRepository.findFirstPreferenceByUserId(userId)
                .map(preference -> selectRandomTrips(
                        queryRepository.findSameDestinationTrips(userId, timeProvider.today(), preference)
                ))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<SameAgeTripResponse> retrieveSameAgeTrips(Long userId) {
        LocalDate today = timeProvider.today();
        int userAge = profileRepository.findBirthdayByUserId(userId)
                .map(birthday -> AgeCalculator.calculate(birthday, today))
                .orElseThrow(BirthdayNotFoundException::new);

        return selectRandomTrips(queryRepository.findSameAgeTrips(userId, today, userAge));
    }

    private <T> List<T> selectRandomTrips(List<T> candidates) {
        List<T> shuffledTrips = new ArrayList<>(candidates);
        Collections.shuffle(shuffledTrips);
        return List.copyOf(shuffledTrips.subList(0, Math.min(HOME_TRIP_DISPLAY_LIMIT, shuffledTrips.size())));
    }
}
