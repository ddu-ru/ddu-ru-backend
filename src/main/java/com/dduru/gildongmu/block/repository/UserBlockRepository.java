package com.dduru.gildongmu.block.repository;

import com.dduru.gildongmu.block.domain.UserBlock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlocker_IdAndBlockedUser_Id(Long blockerUserId, Long blockedUserId);

    @Query("""
            SELECT COUNT(b) > 0
            FROM UserBlock b
            WHERE (b.blocker.id = :firstUserId AND b.blockedUser.id = :secondUserId)
               OR (b.blocker.id = :secondUserId AND b.blockedUser.id = :firstUserId)
            """)
    boolean existsBetween(@Param("firstUserId") Long firstUserId, @Param("secondUserId") Long secondUserId);

    @Modifying
    @Query("DELETE FROM UserBlock b WHERE b.blocker.id = :blockerUserId AND b.blockedUser.id = :blockedUserId")
    int deleteByBlockerUserIdAndBlockedUserId(
            @Param("blockerUserId") Long blockerUserId,
            @Param("blockedUserId") Long blockedUserId
    );

    @Query("""
            SELECT b
            FROM UserBlock b
            JOIN FETCH b.blockedUser u
            LEFT JOIN FETCH u.profile p
            LEFT JOIN FETCH p.avatar
            LEFT JOIN FETCH p.bgColor
            WHERE b.blocker.id = :blockerUserId
              AND (:cursor IS NULL OR b.id < :cursor)
            ORDER BY b.id DESC
            """)
    List<UserBlock> findBlockedUsers(
            @Param("blockerUserId") Long blockerUserId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
