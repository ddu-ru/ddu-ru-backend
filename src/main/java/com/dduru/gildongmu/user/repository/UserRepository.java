package com.dduru.gildongmu.user.repository;

import com.dduru.gildongmu.auth.exception.UserNotFoundException;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthIdAndOauthType(String oauthId, OauthType oauthType);
    boolean existsByOauthIdAndOauthType(String oauthId, OauthType oauthType);
    boolean existsByEmail(String email);

    @Query(
            value = "SELECT u FROM User u LEFT JOIN FETCH u.profile",
            countQuery = "SELECT COUNT(u) FROM User u"
    )
    Page<User> findAllWithProfile(Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.id = :id")
    Optional<User> findWithProfileById(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.id IN :ids")
    List<User> findAllWithProfileByIdIn(@Param("ids") Collection<Long> ids);

    default User getWithProfileByIdOrThrow(Long id) {
        return findWithProfileById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    default User getByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    default User getByIdWithLockOrThrow(Long id) {
        return findByIdWithLock(id)
                .orElseThrow(UserNotFoundException::new);
    }

    @Query("SELECT u.id FROM User u WHERE u.id IN :ids AND u.notificationEnabled = true")
    List<Long> findEnabledUserIds(@Param("ids") List<Long> ids);

    boolean existsByIdAndNotificationEnabled(Long id, boolean notificationEnabled);
}
