package com.github.yildizmy.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A browser that receives the negative-balance alert by Web Push. */
@Getter
@Setter
@Entity
@Table(name = "push_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 1000, nullable = false, unique = true)
    private String endpoint;

    @Column(length = 100, nullable = false)
    private String p256dh;

    @Column(length = 50, nullable = false)
    private String auth;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
