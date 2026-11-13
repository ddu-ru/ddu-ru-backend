package com.dduru.gildongmu.like.repository;

import com.dduru.gildongmu.like.domain.PostLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    Set<Long> findLikedPostIdsByUserId(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Query("SELECT pl.user.id FROM PostLike pl WHERE pl.post.id = :postId")
    List<Long> findUserIdsByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT pl FROM PostLike pl
            JOIN FETCH pl.post post
            JOIN FETCH post.destination
            WHERE pl.user.id = :userId
              AND post.isDeleted = false
              AND (:cursor IS NULL OR pl.id < :cursor)
              AND NOT EXISTS (
                  SELECT ub.id FROM UserBlock ub
                  WHERE (ub.blocker.id = :userId AND ub.blockedUser.id = post.user.id)
                     OR (ub.blocker.id = post.user.id AND ub.blockedUser.id = :userId)
              )
            ORDER BY pl.id DESC
            """)
    List<PostLike> findLikedPostsByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
