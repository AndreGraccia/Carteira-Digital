package com.github.yildizmy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** The user's choice to be warned by e-mail, and which negative window was already announced. */
@Getter
@Setter
@Entity
@Table(name = "future_alert")
public class FutureAlert {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_window_start")
    private LocalDate lastWindowStart;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
