package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderById(Long userId);

    long countByUserId(Long userId);

    /** Scoped by user: a goal id from another account is simply not found. */
    Optional<Goal> findByIdAndUserId(Long id, Long userId);
}
