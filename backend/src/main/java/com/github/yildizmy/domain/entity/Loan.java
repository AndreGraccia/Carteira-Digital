package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Loan contract received from Open Finance (sandbox), shown as plain facts: what is left and when.
 */
@Getter
@Setter
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 160, nullable = false)
    private String externalId;

    @Column(length = 100, nullable = false)
    private String institution;

    @Column(length = 80, nullable = false)
    private String product;

    @Column(nullable = false)
    private BigDecimal contractedAmount;

    @Column(nullable = false)
    private BigDecimal outstandingBalance;

    @Column(nullable = false)
    private BigDecimal installmentAmount;

    @Column(nullable = false)
    private int installmentsTotal;

    @Column(nullable = false)
    private int installmentsPaid;

    private LocalDate nextDueDate;

    private BigDecimal annualRate;

    /** Contractual day of month; the next due date alone loses it after a short month. */
    private Integer dueDay;

    /** Open Finance amortizationScheduled: SAC, PRICE, SAM, SEM_SISTEMA_AMORTIZACAO, OUTROS. */
    @Column(length = 30)
    private String amortizationSystem;

    /** Custo Efetivo Total, fraction per year as in the Loans API (0.29 = 29% a.a.). */
    private BigDecimal cet;

    @Column(nullable = false)
    private Instant lastSyncedAt;
}
