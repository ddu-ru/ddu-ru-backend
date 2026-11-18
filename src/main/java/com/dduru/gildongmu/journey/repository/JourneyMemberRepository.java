package com.dduru.gildongmu.journey.repository;

import com.dduru.gildongmu.journey.domain.JourneyMember;
import com.dduru.gildongmu.journey.domain.enums.JourneyMemberStatus;
import com.dduru.gildongmu.journey.dto.query.JourneyMemberStatusQueryResult;
import com.dduru.gildongmu.journey.dto.query.UpcomingTripMemberQueryResult;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JourneyMemberRepository extends JpaRepository<JourneyMember, Long> {

    Optional<JourneyMember> findByJourneyIdAndUserId(Long journeyId, Long userId);

    boolean existsByJourneyIdAndUserIdAndStatus(Long journeyId, Long userId, JourneyMemberStatus status);

    int countByJourneyIdAndStatus(Long journeyId, JourneyMemberStatus status);

    @Query("""
            SELECT jm.user.id
            FROM JourneyMember jm
            WHERE jm.journey.id = :journeyId
              AND jm.role = 'HOST'
              AND jm.status = 'ACTIVE'
            """)
    Optional<Long> findActiveHostUserIdByJourneyId(@Param("journeyId") Long journeyId);

    @Query("""
            SELECT COUNT(jm) > 0
            FROM JourneyMember jm
            WHERE jm.journey.id = :journeyId
              AND jm.user.id = :userId
              AND jm.role = 'HOST'
              AND jm.status = 'ACTIVE'
            """)
    boolean existsActiveHost(
            @Param("journeyId") Long journeyId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT jm
            FROM JourneyMember jm
            JOIN FETCH jm.journey j
            JOIN FETCH j.post
            JOIN FETCH jm.user
            WHERE j.id = :journeyId
              AND jm.user.id = :userId
              AND jm.status = 'ACTIVE'
            """)
    Optional<JourneyMember> findActiveMemberWithLock(
            @Param("journeyId") Long journeyId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT jm.status
            FROM JourneyMember jm
            WHERE jm.journey.post.id = :postId
              AND jm.user.id = :userId
            """)
    Optional<JourneyMemberStatus> findStatusByPostIdAndUserId(
            @Param("postId") Long postId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT new com.dduru.gildongmu.journey.dto.query.JourneyMemberStatusQueryResult(jm.journey.post.id, jm.status)
            FROM JourneyMember jm
            WHERE jm.journey.post.id IN :postIds
              AND jm.user.id = :userId
            """)
    List<JourneyMemberStatusQueryResult> findStatusesByPostIdsAndUserId(
            @Param("postIds") Collection<Long> postIds,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT jm
            FROM JourneyMember jm
            JOIN FETCH jm.journey j
            JOIN FETCH j.post p
            JOIN FETCH p.destination
            WHERE jm.user.id = :userId
              AND jm.status = :status
              AND p.isDeleted = false
              AND p.endDate >= :today
            ORDER BY p.startDate ASC, j.id DESC
            """)
    List<JourneyMember> findActiveJourneyMembersByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") JourneyMemberStatus status,
            @Param("today") LocalDate today
    );

    @Query("""
            SELECT jm
            FROM JourneyMember jm
            JOIN FETCH jm.journey j
            JOIN FETCH j.post p
            JOIN FETCH p.destination
            WHERE jm.user.id = :userId
              AND jm.status = :status
              AND p.isDeleted = false
              AND p.endDate < :today
            ORDER BY p.endDate DESC, j.id DESC
            """)
    List<JourneyMember> findCompletedJourneyMembersByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") JourneyMemberStatus status,
            @Param("today") LocalDate today
    );

    @Query("""
            SELECT jm
            FROM JourneyMember jm
            JOIN FETCH jm.user u
            JOIN FETCH u.profile pr
            LEFT JOIN FETCH pr.avatar
            LEFT JOIN FETCH pr.bgColor
            WHERE jm.journey.post.id = :postId
              AND jm.status = :status
            ORDER BY jm.role ASC,
                     jm.joinedAt ASC,
                     jm.id ASC
            """)
    List<JourneyMember> findByPostIdAndStatusWithMemberProfiles(
            @Param("postId") Long postId,
            @Param("status") JourneyMemberStatus status
    );

    @Query("""
            SELECT jm
            FROM JourneyMember jm
            JOIN FETCH jm.user u
            JOIN FETCH u.profile pr
            LEFT JOIN FETCH pr.avatar
            LEFT JOIN FETCH pr.bgColor
            WHERE jm.journey.post.id = :postId
              AND jm.status = :status
              AND (:viewerUserId IS NULL OR NOT EXISTS (
                  SELECT ub.id FROM UserBlock ub
                  WHERE (ub.blocker.id = :viewerUserId AND ub.blockedUser.id = jm.user.id)
                     OR (ub.blocker.id = jm.user.id AND ub.blockedUser.id = :viewerUserId)
              ))
            ORDER BY jm.role ASC,
                     jm.joinedAt ASC,
                     jm.id ASC
            """)
    List<JourneyMember> findVisibleByPostIdAndStatusWithMemberProfiles(
            @Param("postId") Long postId,
            @Param("status") JourneyMemberStatus status,
            @Param("viewerUserId") Long viewerUserId
    );

    @Query("""
            SELECT jm.user.id
            FROM JourneyMember jm
            WHERE jm.journey.id = :journeyId
              AND jm.status = 'ACTIVE'
              AND jm.user.id <> :excludeUserId
            """)
    List<Long> findMemberIdsExcludingActor(
            @Param("journeyId") Long journeyId,
            @Param("excludeUserId") Long excludeUserId
    );

    // JPQL new — 엔티티 전체 로딩 없이 필요한 필드만 DTO 생성자로 직접 매핑 (FQCN 필수)
    @Query("""
            SELECT new com.dduru.gildongmu.journey.dto.query.UpcomingTripMemberQueryResult(
                jm.journey.id, jm.journey.title, jm.user.id
            )
            FROM JourneyMember jm
            WHERE jm.journey.post.startDate = :targetDate
              AND jm.status = 'ACTIVE'
            """)
    List<UpcomingTripMemberQueryResult> findUpcomingTripMembers(@Param("targetDate") LocalDate targetDate);
}
