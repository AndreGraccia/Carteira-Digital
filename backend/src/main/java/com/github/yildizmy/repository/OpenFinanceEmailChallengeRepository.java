package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.OpenFinanceEmailChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OpenFinanceEmailChallengeRepository extends JpaRepository<OpenFinanceEmailChallenge, Long> {

    Optional<OpenFinanceEmailChallenge> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from OpenFinanceEmailChallenge challenge where challenge.user.id = :userId")
    Optional<OpenFinanceEmailChallenge> findByUserIdForUpdate(@Param("userId") Long userId);
}
