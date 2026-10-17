package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OpenFinanceConnectionRepository extends JpaRepository<OpenFinanceConnection, Long> {
    /** Users with at least one authorised connection; the sync itself re-checks expiry. */
    @Query("select distinct c.user.id from OpenFinanceConnection c where c.status = :status order by c.user.id")
    List<Long> findUserIdsByStatus(@Param("status") OpenFinanceConnectionStatus status);

    Optional<OpenFinanceConnection> findFirstByUserIdOrderByIdDesc(Long userId);
    Optional<OpenFinanceConnection> findByUserIdAndInstitutionId(Long userId, String institutionId);
}
