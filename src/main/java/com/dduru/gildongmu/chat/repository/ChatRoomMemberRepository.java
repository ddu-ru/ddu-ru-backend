package com.dduru.gildongmu.chat.repository;

import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.ChatRoomMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    int countByRoom(ChatRoom room);

    @Query("""
            SELECT m
            FROM ChatRoomMember m
            JOIN FETCH m.user u
            LEFT JOIN FETCH u.profile
            LEFT JOIN FETCH m.lastReadMessage
            WHERE m.room.id = :roomId
              AND m.user.id = :userId
            """)
    Optional<ChatRoomMember> findByRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM ChatRoomMember m
            LEFT JOIN FETCH m.lastReadMessage
            WHERE m.room.id = :roomId
              AND m.user.id = :userId
            """)
    Optional<ChatRoomMember> findByRoomIdAndUserIdWithLock(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT m
            FROM ChatRoomMember m
            JOIN FETCH m.user u
            LEFT JOIN FETCH u.profile
            WHERE m.room.id = :roomId
            """)
    List<ChatRoomMember> findByRoomIdWithUserProfile(@Param("roomId") Long roomId);

    @Query("""
            SELECT m
            FROM ChatRoomMember m
            JOIN FETCH m.user u
            LEFT JOIN FETCH u.profile p
            LEFT JOIN FETCH p.avatar
            LEFT JOIN FETCH p.bgColor
            WHERE m.room.id IN :roomIds
            """)
    List<ChatRoomMember> findByRoomIdsWithUserProfileImage(@Param("roomIds") List<Long> roomIds);

    @Query("""
            SELECT m
            FROM ChatRoomMember m
            JOIN FETCH m.user
            LEFT JOIN FETCH m.lastReadMessage
            WHERE m.room.id = :roomId
            """)
    List<ChatRoomMember> findByRoomIdWithLastReadMessage(@Param("roomId") Long roomId);

    @Query("""
            SELECT m.user.id
            FROM ChatRoomMember m
            WHERE m.room.id = :roomId
            """)
    List<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);

    @Query("""
            SELECT m.user.id
            FROM ChatRoomMember m
            WHERE m.room.id = :roomId
              AND m.notificationEnabled = true
            """)
    List<Long> findNotificationEnabledUserIdsByRoomId(@Param("roomId") Long roomId);

    @Query("""
            SELECT COUNT(m) > 0
            FROM ChatRoomMember m
            WHERE m.room.id = :roomId
              AND m.user.id = :participantUserId
              AND m.room.status = 'ACTIVE'
            """)
    boolean isMember(@Param("roomId") Long roomId, @Param("participantUserId") Long participantUserId);
}
