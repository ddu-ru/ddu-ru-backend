package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.journey.domain.JourneyPollOption;
import com.dduru.gildongmu.journey.dto.query.JourneyPollOptionCountQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JourneyPollOptionRepository extends JpaRepository<JourneyPollOption, Long> {
    List<JourneyPollOption> findByPollIdOrderByDisplayOrderAsc(Long pollId);

    @Query("""
            SELECT new com.dduru.gildongmu.journey.dto.query.JourneyPollOptionCountQueryResult(
                o.poll.id, o.id, o.content, COUNT(v.id))
            FROM JourneyPollOption o
            LEFT JOIN JourneyPollVote v ON v.option = o
            WHERE o.poll.id IN :pollIds
            GROUP BY o.poll.id, o.id, o.content, o.displayOrder
            ORDER BY o.poll.id, o.displayOrder
            """)
    List<JourneyPollOptionCountQueryResult> countVotesByPollIds(@Param("pollIds") Collection<Long> pollIds);
}
