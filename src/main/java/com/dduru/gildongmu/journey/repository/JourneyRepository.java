package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberRole;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.exception.JourneyNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourneyRepository extends JpaRepository<Journey, Long> {

    Optional<Journey> findByPostId(Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT j
            FROM Journey j
            WHERE j.id = :journeyId
            """)
    Optional<Journey> findByIdWithLock(@Param("journeyId") Long journeyId);

    @Query("""
            SELECT j
            FROM JourneyMember jm
            JOIN jm.journey j
            WHERE j.id = :journeyId
              AND jm.user.id = :userId
              AND jm.status = :status
              AND jm.role = :role
            """)
    Optional<Journey> findUpdatableJourneyByIdAndUserId(
            @Param("journeyId") Long journeyId,
            @Param("userId") Long userId,
            @Param("status") JourneyMemberStatus status,
            @Param("role") JourneyMemberRole role
    );

    @Query("""
            SELECT j.post.id
            FROM Journey j
            WHERE j.id = :journeyId
            """)
    Optional<Long> findPostIdById(@Param("journeyId") Long journeyId);

    @Query("""
            SELECT j
            FROM Journey j
            JOIN FETCH j.post p
            JOIN FETCH p.destination
            WHERE j.id = :journeyId
            """)
    Optional<Journey> findByIdWithPostContext(@Param("journeyId") Long journeyId);

    @Query("""
            SELECT j
            FROM JourneyMember jm
            JOIN jm.journey j
            JOIN FETCH j.post p
            WHERE jm.user.id = :userId
              AND jm.status = 'ACTIVE'
              AND p.isDeleted = false
              AND p.endDate >= :today
            ORDER BY p.startDate ASC, j.id ASC
            """)
    List<Journey> findCurrentAndUpcomingJourneys(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END
            FROM JourneyMember jm
            JOIN jm.journey j
            JOIN j.post p
            WHERE jm.user.id = :userId
              AND jm.status = 'ACTIVE'
              AND p.isDeleted = false
              AND p.endDate >= :today
            """)
    boolean existsCurrentOrUpcomingJourney(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    default Optional<Journey> findNearestCurrentOrUpcomingJourney(Long userId, LocalDate today) {
        return findCurrentAndUpcomingJourneys(userId, today, Pageable.ofSize(1))
                .stream()
                .findFirst();
    }

    default Journey getByIdOrThrow(Long journeyId) {
        return findById(journeyId).orElseThrow(JourneyNotFoundException::new);
    }

    default Journey getByIdWithLockOrThrow(Long journeyId) {
        return findByIdWithLock(journeyId).orElseThrow(JourneyNotFoundException::new);
    }

    default Journey getByPostIdOrThrow(Long postId) {
        return findByPostId(postId).orElseThrow(JourneyNotFoundException::new);
    }

    default Long getPostIdByIdOrThrow(Long journeyId) {
        return findPostIdById(journeyId)
                .orElseThrow(JourneyNotFoundException::new);
    }

    default Journey getByIdWithPostContextOrThrow(Long journeyId) {
        return findByIdWithPostContext(journeyId)
                .orElseThrow(JourneyNotFoundException::new);
    }
}
