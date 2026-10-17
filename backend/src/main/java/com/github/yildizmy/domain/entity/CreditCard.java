package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Credit card received from Open Finance (sandbox). Invoices are rebuilt from {@link CardCharge}s.
 */
@Getter
@Setter
@Entity
@Table(name = "credit_card")
public class CreditCard {

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

    @Column(length = 60, nullable = false)
    private String name;

    @Column(length = 4)
    private String lastDigits;

    @Column(nullable = false)
    private BigDecimal creditLimit;

    @Column(nullable = false)
    private int closingDay;

    @Column(nullable = false)
    private int dueDay;

    /** Next bill as the institution reports it; below the minimum the bill goes into revolving credit. */
    private java.time.LocalDate nextBillDueDate;

    private BigDecimal nextBillTotal;

    private BigDecimal nextBillMinimum;

    @Column(nullable = false)
    private Instant lastSyncedAt;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("purchaseDate DESC, id DESC")
    private List<CardCharge> charges = new ArrayList<>();
}
