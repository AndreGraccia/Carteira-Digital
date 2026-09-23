package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.FutureAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FutureAlertRepository extends JpaRepository<FutureAlert, Long> {

    List<FutureAlert> findByEnabledTrue();
}
