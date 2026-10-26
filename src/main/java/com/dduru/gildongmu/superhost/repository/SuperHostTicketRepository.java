package com.dduru.gildongmu.superhost.repository;

import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SuperHostTicketRepository extends JpaRepository<SuperHostTicket, Long> {
    boolean existsByUser_IdAndSource(Long userId, SuperHostTicketSource source);

    long countByUser_IdAndStatus(Long userId, SuperHostTicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SuperHostTicket> findFirstByUser_IdAndStatusOrderByIdAsc(Long userId, SuperHostTicketStatus status);
}
