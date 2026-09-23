package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.RecurrenceDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurrenceDecisionRepository extends JpaRepository<RecurrenceDecision, Long> {

    List<RecurrenceDecision> findByUserId(Long userId);

    Optional<RecurrenceDecision> findByUserIdAndPatternKey(Long userId, String patternKey);

}
