package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.MarketRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MarketRateRepository extends JpaRepository<MarketRate, Long> {

    @Query("SELECT r FROM MarketRate r WHERE r.modality = :modality AND r.periodStart = "
            + "(SELECT max(o.periodStart) FROM MarketRate o WHERE o.modality = :modality)")
    List<MarketRate> findLatest(@Param("modality") String modality);

    @Modifying
    @Query("DELETE FROM MarketRate r WHERE r.periodStart = :periodStart")
    void deletePeriod(@Param("periodStart") LocalDate periodStart);
}
