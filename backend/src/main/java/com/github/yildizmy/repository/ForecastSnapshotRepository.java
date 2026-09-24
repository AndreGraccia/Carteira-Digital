package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.ForecastSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, Long> {

    Optional<ForecastSnapshot> findByUserIdAndTakenOn(Long userId, LocalDate takenOn);

    List<ForecastSnapshot> findByUserIdAndTakenOnGreaterThanEqualOrderByTakenOn(Long userId, LocalDate from);

    List<ForecastSnapshot> findByTakenOnGreaterThanEqualOrderByUserIdAscTakenOnAsc(LocalDate from);
}
