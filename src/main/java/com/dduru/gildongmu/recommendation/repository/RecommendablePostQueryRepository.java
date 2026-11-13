package com.dduru.gildongmu.recommendation.repository;

import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.recommendation.domain.QMateRecommendationPass;
import com.dduru.gildongmu.recommendation.dto.query.DestinationPreferenceFilter;
import com.dduru.gildongmu.recommendation.dto.query.RecommendablePostQueryResult;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dduru.gildongmu.block.repository.UserBlockPredicates.noBlockBetween;
import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.participation.domain.QParticipation.participation;
import static com.dduru.gildongmu.post.domain.QPost.post;
import static com.dduru.gildongmu.report.domain.QReport.report;
import static com.dduru.gildongmu.survey.domain.QTravelTendency.travelTendency;

@Repository
@RequiredArgsConstructor
public class RecommendablePostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<RecommendablePostQueryResult> findRecommendablePosts(
            Long userId,
            LocalDate today,
            Gender applicantGender,
            Integer applicantAge,
            DestinationPreferenceFilter destinationPreferenceFilter
    ) {
        QMateRecommendationPass recommendationPass = new QMateRecommendationPass("recommendationPass");

        return queryFactory
                .select(Projections.constructor(
                        RecommendablePostQueryResult.class,
                        post.id,
                        post.startDate,
                        post.endDate,
                        post.companionType,
                        travelTendency.rhythmScore,
                        travelTendency.energyScore,
                        travelTendency.consumptionScore,
                        travelTendency.decisionScore
                ))
                .from(post)
                .join(post.destination, destination)
                .join(travelTendency).on(travelTendency.user.eq(post.user))
                .where(
                        post.isDeleted.isFalse(),
                        post.status.eq(PostStatus.OPEN),
                        post.endDate.goe(today),
                        post.recruitCount.lt(post.recruitCapacity),
                        post.user.id.ne(userId),
                        genderCondition(applicantGender),
                        ageCondition(applicantAge),
                        destinationCondition(destinationPreferenceFilter),
                        JPAExpressions.selectOne()
                                .from(participation)
                                .where(
                                        participation.post.id.eq(post.id),
                                        participation.user.id.eq(userId)
                                )
                                .notExists(),
                        JPAExpressions.selectOne()
                                .from(recommendationPass)
                                .where(
                                        recommendationPass.post.id.eq(post.id),
                                        recommendationPass.user.id.eq(userId)
                                )
                                .notExists(),
                        JPAExpressions.selectOne()
                                .from(report)
                                .where(
                                        report.post.id.eq(post.id),
                                        report.user.id.eq(userId)
                                )
                                .notExists(),
                        noBlockBetween(userId, post.user.id)
                )
                .fetch();
    }

    private BooleanExpression genderCondition(Gender applicantGender) {
        if (applicantGender == null || applicantGender == Gender.U) {
            return post.preferredGender.eq(Gender.U);
        }
        return post.preferredGender.eq(Gender.U)
                .or(post.preferredGender.eq(applicantGender));
    }

    private BooleanExpression ageCondition(Integer applicantAge) {
        if (applicantAge == null) {
            return post.isAgeAny.isTrue();
        }
        return post.isAgeAny.isTrue()
                .or(
                        post.isAgeAny.isFalse()
                                .and(post.minAge.loe(applicantAge))
                                .and(post.maxAge.goe(applicantAge))
                );
    }

    private BooleanExpression destinationCondition(DestinationPreferenceFilter criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return null;
        }

        BooleanExpression condition = null;
        if (!criteria.countryCodes().isEmpty()) {
            condition = destination.countryCode.in(criteria.countryCodes());
        }
        if (!criteria.destinationIds().isEmpty()) {
            BooleanExpression cityCondition = destination.id.in(criteria.destinationIds());
            condition = condition == null ? cityCondition : condition.or(cityCondition);
        }
        return condition;
    }
}
