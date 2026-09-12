package com.dduru.gildongmu.home.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.home.dto.response.SameAgeTripResponse;
import com.dduru.gildongmu.home.dto.response.UpcomingTripResponse;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.exception.CurrentOrUpcomingJourneyNotFoundException;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.journey.repository.JourneyScheduleRepository;
import com.dduru.gildongmu.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeTripQueryService {

    private final TimeProvider timeProvider;
    private final OnboardingService onboardingService;
    private final JourneyScheduleRepository journeyScheduleRepository;
    private final JourneyRepository journeyRepository;

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
    public List<SameAgeTripResponse> retrieveSameAgeTrips(Long userId) {
        requireOnboarding(userId);
        LocalDate baseStartDate = timeProvider.today().plusDays(12);
        return List.of(
                new SameAgeTripResponse(701L, "제주 로컬 맛집 탐방", "제주도 한라산", baseStartDate.plusDays(1), 3, 4, HomeMockData.THUMBNAIL_URL),
                new SameAgeTripResponse(702L, "부산 감천문화마을 산책", "부산 감천문화마을", baseStartDate.plusDays(4), 2, 5, HomeMockData.THUMBNAIL_URL),
                new SameAgeTripResponse(703L, "전주 한옥마을 먹방", "전주 한옥마을", baseStartDate.plusDays(7), 4, 6, HomeMockData.THUMBNAIL_URL)
        );
    }

    private void requireOnboarding(Long userId) {
        onboardingService.getStatus(userId);
    }
}
