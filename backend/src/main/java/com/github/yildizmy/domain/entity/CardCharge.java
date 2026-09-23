package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One card purchase, possibly split in instalments. {@code firstInvoice} is the due month of instalment 1.
 */
@Getter
@Setter
@Entity
@Table(name = "card_charge")
public class CardCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCard card;

    @Column(length = 160, nullable = false)
    private String externalId;

    @Column(length = 120, nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal installmentAmount;

    @Column(nullable = false)
    private int installments;

    @Column(nullable = false)
    private LocalDate firstInvoice;
}
