package com.github.yildizmy.domain.entity;

import jakarta.persistence.*;
import com.github.yildizmy.domain.enums.RecurrenceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The user's answer to "does this happen every month?". Only CONFIRMED answers feed projections.
 */
@Getter
@Setter
@Entity
@Table(name = "recurrence_decision")
public class RecurrenceDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 120, nullable = false)
    private String patternKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RecurrenceStatus status;

    @Column(length = 20, nullable = false)
    private String category;

    @Column(nullable = false)
    private Instant decidedAt;

    /** The user's own value; null keeps the estimate. Used by the everyday-spending decision. */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
}
