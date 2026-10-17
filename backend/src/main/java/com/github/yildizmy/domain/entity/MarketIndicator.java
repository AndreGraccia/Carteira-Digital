package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Last value of an SGS series: CDI, IPCA_12M or SELIC. */
@Getter
@Setter
@Entity
@Table(name = "market_indicator")
public class MarketIndicator {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private Instant fetchedAt;
}
