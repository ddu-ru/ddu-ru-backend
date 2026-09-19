package com.dduru.gildongmu.home.repository;

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
import static com.dduru.gildongmu.report.domain.QReport.report;

@Repository
@RequiredArgsConstructor
public class HomeTripQueryRepository {

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
                        post.isDeleted.isFalse(),
                        post.status.eq(PostStatus.OPEN),
                        post.recruitCount.lt(post.recruitCapacity),
                        post.endDate.goe(today),
                        post.recruitDeadline.isNull().or(post.recruitDeadline.goe(today)),
                        post.user.id.ne(userId),
                        JPAExpressions.selectOne().from(report)
                                .where(report.post.id.eq(post.id), report.user.id.eq(userId))
                                .notExists()
                )
                .orderBy(post.id.desc())
                .limit(3)
                .fetch();
    }
}
