package com.github.yildizmy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Real balance on a synced day and the projection made from it, to grade the forecast later. */
@Getter
@Setter
@Entity
@Table(name = "forecast_snapshot")
public class ForecastSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "taken_on", nullable = false)
    private LocalDate takenOn;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal balance;

    /** Balances from {@code takenOn} onwards, separated by ";"; null without confirmed answers. */
    @Column(columnDefinition = "TEXT")
    private String projection;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
