package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.Envelope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvelopeRepository extends JpaRepository<Envelope, Long> {

    List<Envelope> findByUserIdOrderById(Long userId);

    long countByUserId(Long userId);

    /** Scoped by user: an envelope id from another account is simply not found. */
    Optional<Envelope> findByIdAndUserId(Long id, Long userId);
}
