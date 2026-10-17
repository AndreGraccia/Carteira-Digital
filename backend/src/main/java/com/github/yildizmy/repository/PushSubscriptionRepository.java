package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUserId(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);
}
