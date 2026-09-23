package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderById(Long userId);

    Optional<Loan> findByUserIdAndExternalId(Long userId, String externalId);

}
