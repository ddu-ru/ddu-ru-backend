package com.dduru.gildongmu.home.repository;

import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.dduru.gildongmu.destination.domain.QDestination.destination;
import static com.dduru.gildongmu.post.domain.QPost.post;

@Repository
@RequiredArgsConstructor
public class HomePopularDestinationRepository {

    private final JPAQueryFactory queryFactory;

    public record DestinationCount(Long destinationId, long count) {}
    public record DestinationTags(Long destinationId, String tags) {}
    private record DefaultDestination(String countryCode, String city) {}

    private static final List<DefaultDestination> DEFAULT_DESTINATIONS = List.of(
            new DefaultDestination("KR", "제주"), new DefaultDestination("KR", "부산"),
            new DefaultDestination("KR", "강릉"), new DefaultDestination("JP", "후쿠오카"),
            new DefaultDestination("JP", "오사카"), new DefaultDestination("KR", "서울"),
            new DefaultDestination("JP", "도쿄"), new DefaultDestination("JP", "교토"),
            new DefaultDestination("TH", "방콕"), new DefaultDestination("VN", "다낭")
    );

    public List<DestinationCount> findRankedDestinationCounts(int limit) {
        return queryFactory.select(Projections.constructor(DestinationCount.class, post.destination.id, post.id.count()))
                .from(post)
                .where(post.isDeleted.isFalse())
                .groupBy(post.destination.id)
                .orderBy(post.id.count().desc(), post.destination.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<Destination> findFallbackDestinations(Collection<Long> excludedIds, int limit) {
        var first = DEFAULT_DESTINATIONS.get(0);
        var priority = new CaseBuilder().when(destination.countryCode.eq(first.countryCode())
                .and(destination.city.eq(first.city()))).then(0);
        for (int i = 1; i < DEFAULT_DESTINATIONS.size(); i++) {
            var candidate = DEFAULT_DESTINATIONS.get(i);
            priority = priority.when(destination.countryCode.eq(candidate.countryCode())
                    .and(destination.city.eq(candidate.city()))).then(i);
        }
        return queryFactory.selectFrom(destination)
                .where(excludedIds.isEmpty() ? null : destination.id.notIn(excludedIds))
                .orderBy(priority.otherwise(DEFAULT_DESTINATIONS.size()).asc(), destination.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<DestinationCount> findOpenCounts(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return queryFactory.select(Projections.constructor(DestinationCount.class, post.destination.id, post.id.count()))
                .from(post)
                .where(post.destination.id.in(ids), post.isDeleted.isFalse(), post.status.eq(PostStatus.OPEN))
                .groupBy(post.destination.id)
                .fetch();
    }

    public List<DestinationTags> findTags(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return queryFactory.select(Projections.constructor(DestinationTags.class, post.destination.id, post.tags))
                .from(post)
                .where(post.destination.id.in(ids), post.isDeleted.isFalse())
                .fetch();
    }

}
