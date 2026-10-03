package com.dduru.gildongmu.profile.repository;

import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.exception.ProfileNotFoundException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser_Id(Long userId);

    default Profile getByUserIdOrThrow(Long userId) {
        return findByUser_Id(userId)
                .orElseThrow(ProfileNotFoundException::new);
    }
    
    boolean existsByNickname(String nickname);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(p) > 0 FROM Profile p WHERE p.nickname = :nickname")
    boolean existsByNicknameWithLock(@Param("nickname") String nickname);
    
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT p.birthday FROM Profile p WHERE p.user.id = :userId")
    Optional<LocalDate> findBirthdayByUserId(@Param("userId") Long userId);
}
