package com.dduru.gildongmu.post.repository;

import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.MyPagePostFilter;
import com.dduru.gildongmu.post.domain.enums.RecruitmentStatusFilter;
import com.dduru.gildongmu.post.domain.enums.PostSortType;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.dto.request.PostListRequest;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.post.domain.QPost.post;
import static com.dduru.gildongmu.profile.domain.QProfile.profile;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private static final int DEADLINE_NEAR_DAYS = 3;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findPostsWithFilters(PostListRequest request, LocalDate today, Integer cursorValue, Pageable pageable) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.destination, destination).fetchJoin()
                .leftJoin(post.user).fetchJoin()
                .leftJoin(post.user.profile, profile).fetchJoin()
                .where(
                        isNotDeleted(),
                        cursorCondition(request.cursor(), cursorValue, request.sort()),
                        keywordCondition(request.keyword()),
                        dateRangeCondition(request.startDate(), request.endDate()),
                        genderCondition(request.preferredGender()),
                        ageRangeCondition(request.minAge(), request.maxAge()),
                        destinationCondition(request.destinationId()),
                        countryCondition(request.countryCode()),
                        recruitCapacityCondition(request.minRecruitCapacity(), request.maxRecruitCapacity()),
                        recruitmentStatusCondition(request.recruitmentStatus(), today),
                        companionTypeCondition(request.companionType())
                )
                .orderBy(sortOrder(request.sort()))
                .limit(pageable.getPageSize())
                .fetch();
    }

    private OrderSpecifier<?>[] sortOrder(PostSortType sort) {
        return switch (sort) {
            case VIEW -> new OrderSpecifier[]{post.viewCount.desc(), post.id.desc()};
            case LIKE -> new OrderSpecifier[]{post.likeCount.desc(), post.id.desc()};
            default -> new OrderSpecifier[]{post.id.desc()};
        };
    }

    @Override
    public List<Post> findPostsByUserId(Long userId, MyPagePostFilter filter, Long cursor, Pageable pageable, LocalDate today) {
        return queryFactory
                .selectFrom(post)
                .leftJoin(post.destination, destination).fetchJoin()
                .where(
                        post.user.id.eq(userId),
                        isNotDeleted(),
                        cursor != null ? post.id.lt(cursor) : null,
                        myPageStatusFilter(filter, today)
                )
                .orderBy(post.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countTotalPostsByUserId(Long userId) {
        Long count = queryFactory
                .select(post.count())
                .from(post)
                .where(post.user.id.eq(userId), isNotDeleted())
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression myPageStatusFilter(MyPagePostFilter filter, LocalDate today) {
        if (filter == MyPagePostFilter.ALL) return null;
        return switch (filter) {
            case RECRUITING -> post.endDate.goe(today)
                    .and(post.status.eq(PostStatus.OPEN))
                    .and(post.recruitCount.lt(post.recruitCapacity));
            case RECRUITMENT_CLOSED -> post.endDate.goe(today)
                    .and(post.status.eq(PostStatus.CLOSED)
                            .or(post.recruitCount.goe(post.recruitCapacity)));
            case TRAVEL_ENDED -> post.endDate.lt(today);
            default -> null;
        };
    }

    private BooleanExpression isNotDeleted() {
        return post.isDeleted.eq(false);
    }

    private BooleanExpression cursorCondition(Long cursorId, Integer cursorValue, PostSortType sort) {
        if (cursorId == null) return null;
        return switch (sort) {
            case VIEW -> cursorValue == null ? null :
                    post.viewCount.lt(cursorValue)
                            .or(post.viewCount.eq(cursorValue).and(post.id.lt(cursorId)));
            case LIKE -> cursorValue == null ? null :
                    post.likeCount.lt(cursorValue)
                            .or(post.likeCount.eq(cursorValue).and(post.id.lt(cursorId)));
            default -> post.id.lt(cursorId);
        };
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null) return null;
        return post.title.containsIgnoreCase(keyword)
                .or(post.content.containsIgnoreCase(keyword))
                .or(destination.city.containsIgnoreCase(keyword))
                .or(destination.countryName.containsIgnoreCase(keyword))
                // tags는 JSON 배열 문자열로 저장되므로 containsIgnoreCase로 태그명 부분 일치 검색
                .or(post.tags.containsIgnoreCase(keyword));
    }

    private BooleanExpression dateRangeCondition(LocalDate startDate, LocalDate endDate) {
        BooleanExpression condition = null;
        if (startDate != null) {
            condition = post.endDate.goe(startDate);
        }
        if (endDate != null) {
            BooleanExpression endCondition = post.startDate.loe(endDate);
            condition = condition != null ? condition.and(endCondition) : endCondition;
        }
        return condition;
    }

    private BooleanExpression genderCondition(Gender preferredGender) {
        if (preferredGender == null || preferredGender == Gender.U) return null;
        return post.preferredGender.eq(preferredGender)
                .or(post.preferredGender.eq(Gender.U));
    }

    private BooleanExpression ageRangeCondition(Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) return null;
        BooleanExpression ageAny = post.isAgeAny.eq(true);
        BooleanExpression inRange = post.isAgeAny.eq(false);
        if (minAge != null) {
            inRange = inRange.and(post.maxAge.goe(minAge));
        }
        if (maxAge != null) {
            inRange = inRange.and(post.minAge.loe(maxAge));
        }
        return ageAny.or(inRange);
    }

    private BooleanExpression destinationCondition(Long destinationId) {
        if (destinationId == null) return null;
        return post.destination.id.eq(destinationId);
    }

    private BooleanExpression countryCondition(String countryCode) {
        if (countryCode == null) return null;
        return destination.countryCode.eq(countryCode);
    }

    private BooleanExpression recruitCapacityCondition(Integer minRecruitCapacity, Integer maxRecruitCapacity) {
        if (minRecruitCapacity == null && maxRecruitCapacity == null) return null;
        BooleanExpression condition = null;
        if (minRecruitCapacity != null) {
            condition = post.recruitCapacity.goe(minRecruitCapacity);
        }
        if (maxRecruitCapacity != null) {
            BooleanExpression maxCondition = post.recruitCapacity.loe(maxRecruitCapacity);
            condition = condition == null ? maxCondition : condition.and(maxCondition);
        }
        return condition;
    }

    private BooleanExpression recruitmentStatusCondition(RecruitmentStatusFilter status, LocalDate today) {
        BooleanExpression openAndNotFull = post.status.eq(PostStatus.OPEN)
                .and(post.recruitCount.lt(post.recruitCapacity));
        if (status == null) return null;
        return switch (status) {
            case OPEN -> openAndNotFull;
            case DEADLINE_NEAR -> openAndNotFull
                    .and(post.recruitDeadline.isNotNull())
                    .and(post.recruitDeadline.between(today, today.plusDays(DEADLINE_NEAR_DAYS)));
            case CLOSED -> post.recruitCount.goe(post.recruitCapacity)
                    .or(post.status.eq(PostStatus.CLOSED));
        };
    }

    private BooleanExpression companionTypeCondition(CompanionType companionType) {
        if (companionType == null) return null;
        return post.companionType.eq(companionType);
    }
}
