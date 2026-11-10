package com.dduru.gildongmu.report.repository;

import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdWithLock(@Param("id") Long id);

    Optional<Report> findByIdAndUserId(Long id, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    boolean existsByTargetTypeAndReportedUserIdAndUserIdAndStatusIn(
            ReportTargetType type, Long targetUserId, Long reporterId, Collection<ReportStatus> statuses);

    @Query(
            value = """
                    SELECT r FROM Report r
                    LEFT JOIN FETCH r.post
                    LEFT JOIN FETCH r.user
                    LEFT JOIN FETCH r.reviewer
                    LEFT JOIN FETCH r.reportedUser
                    LEFT JOIN FETCH r.chatRoom
                    LEFT JOIN FETCH r.message
                    WHERE (:status IS NULL OR r.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM Report r
                    WHERE (:status IS NULL OR r.status = :status)
                    """
    )
    Page<Report> findAllByStatusOptional(@Param("status") ReportStatus status, Pageable pageable);
}
