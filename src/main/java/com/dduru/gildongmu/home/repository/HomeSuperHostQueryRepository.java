package com.dduru.gildongmu.home.repository;

import com.dduru.gildongmu.home.dto.query.HomeSuperHostQueryResult;
import com.dduru.gildongmu.home.dto.query.QHomeSuperHostQueryResult;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostExposureStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.post.domain.QPost.post;
import static com.dduru.gildongmu.profile.domain.QBgColor.bgColor;
import static com.dduru.gildongmu.profile.domain.QProfile.profile;
import static com.dduru.gildongmu.report.domain.QReport.report;
import static com.dduru.gildongmu.superhost.domain.QSuperHostExposure.superHostExposure;
import static com.dduru.gildongmu.survey.domain.QAvatarProfile.avatarProfile;
import static com.dduru.gildongmu.user.domain.QUser.user;

@Repository
@RequiredArgsConstructor
public class HomeSuperHostQueryRepository {

    private static final long HOME_SUPER_HOST_LIMIT = 5L;

    private final JPAQueryFactory queryFactory;

    public List<HomeSuperHostQueryResult> findVisibleSuperHosts(
            Long viewerId,
            LocalDateTime now,
            LocalDate today
    ) {
        return queryFactory
                .select(new QHomeSuperHostQueryResult(
                        post.id,
                        post.title,
                        destination.countryName,
                        destination.city,
                        post.startDate,
                        post.endDate,
                        post.recruitCount,
                        post.recruitCapacity,
                        post.tags,
                        post.viewCount,
                        post.photoUrl,
                        profile.nickname,
                        profile.profileImageType,
                        profile.uploadedImageUrl,
                        avatarProfile.imageUrl,
                        bgColor.id,
                        profile.birthday,
                        profile.gender
                ))
                .from(superHostExposure)
                .join(superHostExposure.post, post)
                .join(post.destination, destination)
                .join(post.user, user)
                .leftJoin(user.profile, profile)
                .leftJoin(profile.avatar, avatarProfile)
                .leftJoin(profile.bgColor, bgColor)
                .where(
                        superHostExposure.status.eq(SuperHostExposureStatus.ACTIVE),
                        superHostExposure.startedAt.loe(now),
                        superHostExposure.endedAt.gt(now),
                        post.isDeleted.isFalse(),
                        post.status.eq(PostStatus.OPEN),
                        post.recruitCount.lt(post.recruitCapacity),
                        post.endDate.goe(today),
                        post.recruitDeadline.isNull().or(post.recruitDeadline.goe(today)),
                        notReportedBy(viewerId)
                )
                .orderBy(superHostExposure.startedAt.desc(), superHostExposure.id.desc())
                .limit(HOME_SUPER_HOST_LIMIT)
                .fetch();
    }

    private BooleanExpression notReportedBy(Long viewerId) {
        if (viewerId == null) {
            return null;
        }
        return JPAExpressions.selectOne()
                .from(report)
                .where(report.post.id.eq(post.id), report.user.id.eq(viewerId))
                .notExists();
    }
}
