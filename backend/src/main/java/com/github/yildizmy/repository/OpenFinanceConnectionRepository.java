package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenFinanceConnectionRepository extends JpaRepository<OpenFinanceConnection, Long> {
    Optional<OpenFinanceConnection> findFirstByUserIdOrderByLastSyncedAtDesc(Long userId);
    Optional<OpenFinanceConnection> findByUserIdAndInstitutionId(Long userId, String institutionId);
}
