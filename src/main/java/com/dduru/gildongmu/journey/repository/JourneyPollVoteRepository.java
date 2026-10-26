package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.journey.domain.JourneyPollVote;
import com.dduru.gildongmu.journey.dto.query.JourneyPollVoterCountQueryResult;
import com.dduru.gildongmu.journey.dto.query.JourneyPollSelectionQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JourneyPollVoteRepository extends JpaRepository<JourneyPollVote, Long> {
    @Query("""
            SELECT new com.dduru.gildongmu.journey.dto.query.JourneyPollVoterCountQueryResult(
                v.option.poll.id, COUNT(DISTINCT v.user.id))
            FROM JourneyPollVote v WHERE v.option.poll.id IN :pollIds
            GROUP BY v.option.poll.id
            """)
    List<JourneyPollVoterCountQueryResult> countVotersByPollIds(@Param("pollIds") Collection<Long> pollIds);

    @Query("""
            SELECT new com.dduru.gildongmu.journey.dto.query.JourneyPollSelectionQueryResult(v.option.poll.id, v.option.id)
            FROM JourneyPollVote v WHERE v.option.poll.id IN :pollIds AND v.user.id = :userId
            ORDER BY v.option.displayOrder
            """)
    List<JourneyPollSelectionQueryResult> findSelectionsByPollIdsAndUserId(
            @Param("pollIds") Collection<Long> pollIds, @Param("userId") Long userId);

    @Query("""
            SELECT v FROM JourneyPollVote v
            JOIN FETCH v.option o
            JOIN FETCH v.user u
            LEFT JOIN FETCH u.profile profile
            LEFT JOIN FETCH profile.avatar
            WHERE o.poll.id = :pollId
            ORDER BY o.displayOrder, v.id
            """)
    List<JourneyPollVote> findByPollIdWithVoter(@Param("pollId") Long pollId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM JourneyPollVote v WHERE v.option.id IN :optionIds AND v.user.id = :userId")
    int deleteByOptionIdsAndUserId(@Param("optionIds") Collection<Long> optionIds, @Param("userId") Long userId);
}
