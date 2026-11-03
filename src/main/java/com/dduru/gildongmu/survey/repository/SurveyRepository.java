package com.dduru.gildongmu.survey.repository;

import com.dduru.gildongmu.survey.domain.Survey;
import com.dduru.gildongmu.survey.exception.SurveyResultNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    boolean existsByUserId(Long userId);

    Optional<Survey> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Survey s where s.user.id = :userId")
    Optional<Survey> findByUserIdForUpdate(@Param("userId") Long userId);

    default Survey getByUserIdForUpdateOrThrow(Long userId) {
        return findByUserIdForUpdate(userId)
                .orElseThrow(SurveyResultNotFoundException::new);
    }

    default Survey getByUserIdOrThrow(Long userId) {
        return findByUserId(userId)
                .orElseThrow(SurveyResultNotFoundException::new);
    }
}
