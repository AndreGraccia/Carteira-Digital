package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findByUserIdOrderById(Long userId);

    Optional<CreditCard> findByUserIdAndExternalId(Long userId, String externalId);

}
