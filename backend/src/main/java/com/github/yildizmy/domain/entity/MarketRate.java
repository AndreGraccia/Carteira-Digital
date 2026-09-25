package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One institution's rate for one week, as published by Banco Central (percent). */
@Getter
@Setter
@Entity
@Table(name = "market_rate")
public class MarketRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String modality;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(length = 8, nullable = false)
    private String cnpj8;

    @Column(length = 120, nullable = false)
    private String institution;

    @Column(nullable = false)
    private BigDecimal monthlyRate;

    @Column(nullable = false)
    private BigDecimal annualRate;
}
