package com.github.yildizmy.domain.entity;

import com.github.yildizmy.domain.enums.Status;
import com.github.yildizmy.domain.enums.TransactionSource;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@EqualsAndHashCode(of = {"referenceNumber"})
public class Transaction {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "transaction_seq_gen"
    )
    @SequenceGenerator(
            name = "transaction_seq_gen",
            sequenceName = "transaction_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    // Names people and companies: encrypted at rest (ADR 0014, V30 widened the column).
    @Convert(converter = com.github.yildizmy.domain.EncryptedText.class)
    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, unique = true)
    private UUID referenceNumber;

    @Column(unique = true)
    private UUID requestId;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionSource source = TransactionSource.LOCAL;

    @Column(length = 255, unique = true)
    private String externalId;

    /** Open Finance {@code type} of an imported entry (PIX, TED, BOLETO...); null for local ones and older imports. */
    @Column(length = 40)
    private String paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id", referencedColumnName = "id", nullable = false)
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id", referencedColumnName = "id", nullable = false)
    private Wallet toWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    private Type type;
}
