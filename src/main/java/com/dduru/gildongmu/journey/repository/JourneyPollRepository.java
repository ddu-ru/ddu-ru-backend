package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.journey.domain.JourneyPoll;
import com.dduru.gildongmu.journey.exception.JourneyPollNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JourneyPollRepository extends JpaRepository<JourneyPoll, Long> {
    @Query("""
            SELECT p FROM JourneyPoll p
            WHERE p.journey.id = :journeyId AND (:cursor IS NULL OR p.id < :cursor)
            ORDER BY p.id DESC
            """)
    List<JourneyPoll> findByJourneyIdWithCursor(@Param("journeyId") Long journeyId,
                                               @Param("cursor") Long cursor, Pageable pageable);

    @Query("""
            SELECT p FROM JourneyPoll p
            JOIN FETCH p.author a
            LEFT JOIN FETCH a.profile profile
            LEFT JOIN FETCH profile.avatar
            WHERE p.id = :pollId AND p.journey.id = :journeyId
            """)
    Optional<JourneyPoll> findByIdAndJourneyIdWithAuthor(@Param("pollId") Long pollId,
                                                       @Param("journeyId") Long journeyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM JourneyPoll p WHERE p.id = :pollId AND p.journey.id = :journeyId")
    Optional<JourneyPoll> findByIdAndJourneyIdWithLock(@Param("pollId") Long pollId,
                                                     @Param("journeyId") Long journeyId);

    default JourneyPoll getByIdAndJourneyIdOrThrow(Long pollId, Long journeyId) {
        return findByIdAndJourneyIdWithAuthor(pollId, journeyId).orElseThrow(JourneyPollNotFoundException::new);
    }

    default JourneyPoll getByIdAndJourneyIdWithLockOrThrow(Long pollId, Long journeyId) {
        return findByIdAndJourneyIdWithLock(pollId, journeyId).orElseThrow(JourneyPollNotFoundException::new);
    }
}
