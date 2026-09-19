package com.dduru.gildongmu.recommendation.repository;

import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.profile.domain.QBgColor;
import com.dduru.gildongmu.profile.domain.QProfile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.recommendation.domain.QMateRecommendationPass;
import com.dduru.gildongmu.recommendation.dto.query.DestinationPreferenceFilter;
import com.dduru.gildongmu.recommendation.dto.query.MateRecommendationCardQueryResult;
import com.dduru.gildongmu.recommendation.dto.query.RecommendationApplicantContext;
import com.dduru.gildongmu.survey.domain.QAvatarProfile;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.participation.domain.QParticipation.participation;
import static com.dduru.gildongmu.post.domain.QPost.post;
import static com.dduru.gildongmu.recommendation.domain.QMateRecommendation.mateRecommendation;
import static com.dduru.gildongmu.recommendation.domain.QMateRecommendationBatch.mateRecommendationBatch;
import static com.dduru.gildongmu.report.domain.QReport.report;
import static com.dduru.gildongmu.user.domain.QUser.user;

@Repository
@RequiredArgsConstructor
public class MateRecommendationCardQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MateRecommendationCardQueryResult> findVisibleCards(
            Long batchId,
            Long userId,
            RecommendationApplicantContext context
    ) {
        QProfile hostProfile = new QProfile("hostProfile");
        QAvatarProfile hostAvatar = new QAvatarProfile("hostAvatar");
        QBgColor hostBgColor = new QBgColor("hostBgColor");
        QMateRecommendationPass recommendationPass = new QMateRecommendationPass("visibleRecommendationPass");

        return queryFactory
                .select(Projections.constructor(
                        MateRecommendationCardQueryResult.class,
                        mateRecommendation.id,
                        post.id,
                        mateRecommendation.recommendationRank,
                        mateRecommendation.matchPercentage,
                        mateRecommendation.matchReasons,
                        mateRecommendation.cautionPoints,
                        post.title,
                        post.photoUrl,
                        destination.countryName,
                        destination.city,
                        post.startDate,
                        post.endDate,
                        post.companionType,
                        post.recruitCount,
                        post.recruitCapacity,
                        post.content,
                        post.tags,
                        hostProfile.nickname,
                        hostProfile.profileImageType,
                        hostProfile.uploadedImageUrl,
                        hostAvatar.imageUrl,
                        hostBgColor.id,
                        hostProfile.birthday,
                        hostProfile.gender
                ))
                .from(mateRecommendation)
                .join(mateRecommendation.batch, mateRecommendationBatch)
                .join(mateRecommendation.post, post)
                .join(post.destination, destination)
                .join(post.user, user)
                .join(hostProfile).on(hostProfile.user.eq(user))
                .leftJoin(hostProfile.avatar, hostAvatar)
                .leftJoin(hostProfile.bgColor, hostBgColor)
                .where(
                        mateRecommendationBatch.id.eq(batchId),
                        mateRecommendationBatch.user.id.eq(userId),
                        post.isDeleted.isFalse(),
                        post.status.eq(PostStatus.OPEN),
                        post.endDate.goe(context.today()),
                        post.recruitCount.lt(post.recruitCapacity),
                        post.user.id.ne(userId),
                        hostProfile.nickname.isNotNull(),
                        hostProfile.birthday.isNotNull(),
                        hostProfile.gender.isNotNull(),
                        genderCondition(context.gender()),
                        ageCondition(context.age()),
                        destinationCondition(context.destinationPreferenceFilter()),
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
                                .notExists()
                )
                .orderBy(mateRecommendation.recommendationRank.asc())
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
