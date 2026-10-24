package com.dduru.gildongmu.superhost.repository;

import com.dduru.gildongmu.superhost.domain.SuperHostExposure;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostExposureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SuperHostExposureRepository extends JpaRepository<SuperHostExposure, Long> {
    boolean existsByPost_IdAndStatusAndEndedAtAfter(Long postId, SuperHostExposureStatus status, LocalDateTime now);

    @Query("""
            SELECT MAX(e.endedAt)
            FROM SuperHostExposure e
            WHERE e.post.id = :postId
              AND e.status = :status
              AND e.endedAt > :now
            """)
    Optional<LocalDateTime> findLatestActiveEndedAtByPostId(@Param("postId") Long postId,
                                                            @Param("status") SuperHostExposureStatus status,
                                                            @Param("now") LocalDateTime now);

    @Query("""
            SELECT e.post.id, MAX(e.endedAt)
            FROM SuperHostExposure e
            WHERE e.post.id IN :postIds
              AND e.status = :status
              AND e.endedAt > :now
            GROUP BY e.post.id
            """)
    List<Object[]> findActivePostIdsWithEndedAt(@Param("postIds") Collection<Long> postIds,
                                                @Param("status") SuperHostExposureStatus status,
                                                @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SuperHostExposure e
            SET e.status = :endedStatus
            WHERE e.status = :activeStatus
              AND e.endedAt <= :now
            """)
    int endExpiredExposures(@Param("activeStatus") SuperHostExposureStatus activeStatus,
                            @Param("endedStatus") SuperHostExposureStatus endedStatus,
                            @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE SuperHostExposure e
            SET e.status = :endedStatus
            WHERE e.post.id = :postId
              AND e.status = :activeStatus
              AND e.endedAt <= :now
            """)
    int endExpiredExposureByPostId(@Param("postId") Long postId,
                                   @Param("activeStatus") SuperHostExposureStatus activeStatus,
                                   @Param("endedStatus") SuperHostExposureStatus endedStatus,
                                   @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SuperHostExposure e
            SET e.status = :cancelledStatus,
                e.endedAt = :now
            WHERE e.post.id = :postId
              AND e.status = :activeStatus
              AND e.endedAt > :now
            """)
    int cancelActiveExposureByPostId(@Param("postId") Long postId,
                                     @Param("activeStatus") SuperHostExposureStatus activeStatus,
                                     @Param("cancelledStatus") SuperHostExposureStatus cancelledStatus,
                                     @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SuperHostExposure e
            SET e.status = :cancelledStatus,
                e.endedAt = :now
            WHERE e.status = :activeStatus
              AND e.endedAt > :now
              AND e.post.status = com.dduru.gildongmu.post.domain.enums.PostStatus.CLOSED
            """)
    int cancelActiveExposureByClosedPosts(@Param("activeStatus") SuperHostExposureStatus activeStatus,
                                          @Param("cancelledStatus") SuperHostExposureStatus cancelledStatus,
                                          @Param("now") LocalDateTime now);
}
