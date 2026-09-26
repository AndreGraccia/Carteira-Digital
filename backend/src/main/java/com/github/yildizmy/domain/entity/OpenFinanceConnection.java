package com.github.yildizmy.domain.entity;

import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "open_finance_connection", uniqueConstraints = {
        @UniqueConstraint(name = "open_finance_user_institution_key", columnNames = {"user_id", "institution_id"})
})
public class OpenFinanceConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "open_finance_connection_seq_gen")
    @SequenceGenerator(
            name = "open_finance_connection_seq_gen",
            sequenceName = "open_finance_connection_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "institution_id", length = 40, nullable = false)
    private String institutionId;

    @Column(length = 100, nullable = false)
    private String institutionName;

    @Column(length = 100, nullable = false)
    private String consentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private OpenFinanceConnectionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant authorisedAt;

    /** Consent expirationDateTime, chosen when the consent is created; a sync never moves it. */
    private Instant expiresAt;

    private Instant lastSyncedAt;

    /** USER, ASPSP or TPP, when REJECTED. */
    @Column(length = 10)
    private String rejectedBy;

    /** Open Finance rejection reason code, e.g. CUSTOMER_MANUALLY_REVOKED, CONSENT_MAX_DATE_REACHED. */
    @Column(length = 40)
    private String rejectionReason;

    private Instant rejectedAt;

    public void reject(String by, String reason, Instant at) {
        this.status = OpenFinanceConnectionStatus.REJECTED;
        this.rejectedBy = by;
        this.rejectionReason = reason;
        this.rejectedAt = at;
    }
}
