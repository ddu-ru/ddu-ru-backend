package com.dduru.gildongmu.chat.repository;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
            SELECT m
            FROM ChatMessage m
            LEFT JOIN FETCH m.sender s
            LEFT JOIN FETCH s.profile
            LEFT JOIN FETCH m.poll
            WHERE m.room.id = :roomId
              AND m.createdAt >= :visibleFrom
              AND (:beforeMessageId IS NULL OR m.id < :beforeMessageId)
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findVisibleMessages(
            @Param("roomId") Long roomId,
            @Param("beforeMessageId") Long beforeMessageId,
            @Param("visibleFrom") LocalDateTime visibleFrom,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.id = :messageId
              AND m.room.id = :roomId
            """)
    Optional<ChatMessage> findByIdAndRoomId(
            @Param("messageId") Long messageId,
            @Param("roomId") Long roomId
    );

    boolean existsByIdAndRoom_IdAndCreatedAtGreaterThanEqual(Long messageId, Long roomId, LocalDateTime visibleFrom);

    @Query("""
            SELECT m
            FROM ChatMessage m
            JOIN FETCH m.sender s
            JOIN FETCH s.profile p
            JOIN FETCH m.room r
            LEFT JOIN FETCH r.journey j
            WHERE m.id = :messageId
            """)
    Optional<ChatMessage> findByIdWithSenderAndRoom(@Param("messageId") Long messageId);
}
