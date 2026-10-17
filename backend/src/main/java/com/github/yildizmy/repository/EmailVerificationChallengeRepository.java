package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.EmailVerificationChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, Long> {

    Optional<EmailVerificationChallenge> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from EmailVerificationChallenge challenge where challenge.user.id = :userId")
    Optional<EmailVerificationChallenge> findByUserIdForUpdate(@Param("userId") Long userId);
}
