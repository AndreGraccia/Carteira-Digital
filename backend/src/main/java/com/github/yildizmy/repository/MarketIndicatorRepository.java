package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.MarketIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketIndicatorRepository extends JpaRepository<MarketIndicator, String> {
}
