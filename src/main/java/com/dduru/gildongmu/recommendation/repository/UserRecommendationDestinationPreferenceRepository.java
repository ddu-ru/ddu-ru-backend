package com.dduru.gildongmu.recommendation.repository;

import com.dduru.gildongmu.recommendation.domain.UserRecommendationDestinationPreference;
import com.dduru.gildongmu.recommendation.dto.query.DestinationPreferenceFilterRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRecommendationDestinationPreferenceRepository extends JpaRepository<UserRecommendationDestinationPreference, Long> {
    // 게시글 추천 필터링 전용 - 엔티티 로드 없이 3개 컬럼만 조회
    @Query("""
            SELECT new com.dduru.gildongmu.recommendation.dto.query.DestinationPreferenceFilterRow(
                p.preferenceType,
                p.countryCode,
                d.id
            )
            FROM UserRecommendationDestinationPreference p
            LEFT JOIN p.destination d
            WHERE p.user.id = :userId
            """)
    List<DestinationPreferenceFilterRow> findFilterRowsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM UserRecommendationDestinationPreference p
            LEFT JOIN FETCH p.destination
            WHERE p.user.id = :userId
            ORDER BY p.preferenceRank ASC
            """)
    List<UserRecommendationDestinationPreference> findAllByUserIdWithDestination(@Param("userId") Long userId);

    boolean existsByUser_Id(Long userId);

    @Query("""
            SELECT p FROM UserRecommendationDestinationPreference p
            LEFT JOIN FETCH p.destination
            WHERE p.user.id = :userId AND p.preferenceRank = 1
            """)
    Optional<UserRecommendationDestinationPreference> findFirstPreferenceByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserRecommendationDestinationPreference p WHERE p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
