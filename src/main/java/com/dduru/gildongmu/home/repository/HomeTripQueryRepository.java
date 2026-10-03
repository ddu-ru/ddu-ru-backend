package com.dduru.gildongmu.home.repository;

import com.dduru.gildongmu.home.dto.response.SameAgeTripResponse;
import com.dduru.gildongmu.home.dto.response.SameDestinationTripResponse;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.post.domain.QPost.post;
import static com.dduru.gildongmu.profile.domain.QProfile.profile;
import static com.dduru.gildongmu.report.domain.QReport.report;

@Repository
@RequiredArgsConstructor
public class HomeTripQueryRepository {

    private static final int AGE_TOLERANCE = 5;
    private static final int HOME_TRIP_CANDIDATE_LIMIT = 30;

    private final JPAQueryFactory queryFactory;

    public List<SameDestinationTripResponse> findSameDestinationTrips(
            Long userId, LocalDate today, UserRecommendationDestinationPreference preference
    ) {
        BooleanExpression destinationCondition = preference.getPreferenceType() == RecommendationDestinationPreferenceType.COUNTRY
                ? destination.countryCode.eq(preference.getCountryCode())
                : destination.id.eq(preference.getDestination().getId());

        return queryFactory.select(Projections.constructor(SameDestinationTripResponse.class,
                        post.id, post.title, destination.city, post.startDate, post.endDate,
                        post.recruitCount, post.recruitCapacity, post.photoUrl))
                .from(post)
                .join(post.destination, destination)
                .where(
                        destinationCondition,
                        eligibleTripCondition(userId, today)
                )
                .orderBy(post.createdAt.asc(), post.id.asc())
                .limit(HOME_TRIP_CANDIDATE_LIMIT)
                .fetch();
    }

    public List<SameAgeTripResponse> findSameAgeTrips(Long userId, LocalDate today, int userAge) {
        int minAge = Math.max(0, userAge - AGE_TOLERANCE);
        int maxAge = userAge + AGE_TOLERANCE;
        // 만 나이 범위: (maxAge + 1)세 생일은 제외하고 minAge세 생일은 포함합니다.
        LocalDate birthdayExclusiveLowerBound = today.minusYears(maxAge + 1L);
        LocalDate birthdayInclusiveUpperBound = today.minusYears(minAge);

        return queryFactory.select(Projections.constructor(SameAgeTripResponse.class,
                        post.id, post.title, destination.city, post.startDate, post.endDate,
                        post.recruitCount, post.recruitCapacity, post.photoUrl))
                .from(post)
                .join(post.destination, destination)
                .join(profile).on(profile.user.id.eq(post.user.id))
                .where(
                        profile.birthday.gt(birthdayExclusiveLowerBound),
                        profile.birthday.loe(birthdayInclusiveUpperBound),
                        eligibleTripCondition(userId, today)
                )
                .orderBy(post.createdAt.asc(), post.id.asc())
                .limit(HOME_TRIP_CANDIDATE_LIMIT)
                .fetch();
    }

    private BooleanExpression eligibleTripCondition(Long userId, LocalDate today) {
        return post.isDeleted.isFalse()
                .and(post.status.eq(PostStatus.OPEN))
                .and(post.recruitCount.lt(post.recruitCapacity))
                .and(post.endDate.goe(today))
                .and(post.user.id.ne(userId))
                .and(JPAExpressions.selectOne().from(report)
                        .where(report.post.id.eq(post.id), report.user.id.eq(userId))
                        .notExists());
    }
}
