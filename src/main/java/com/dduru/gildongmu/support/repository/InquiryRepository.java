package com.dduru.gildongmu.support.repository;

import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.domain.enums.InquiryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("select i from Inquiry i where i.user.id = :userId and (:cursor is null or i.id < :cursor) order by i.id desc")
    List<Inquiry> findMine(Long userId, Long cursor, Pageable pageable);

    @Query("select i from Inquiry i where (:status is null or i.status = :status)")
    Page<Inquiry> findByOptionalStatus(InquiryStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inquiry i where i.id = :id")
    Optional<Inquiry> findByIdWithLock(Long id);
}
