package com.github.yildizmy.service.openfinance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OpenFinanceSnapshot(
        String institutionId,
        String institutionName,
        String consentId,
        Instant authorisedAt,
        Instant expiresAt,
        List<Account> accounts
) {
    public record Account(
            String accountId,
            String brandName,
            String type,
            String branchCode,
            String number,
            String checkDigit,
            BigDecimal availableBalance,
            List<Entry> transactions
    ) {}

    public record Entry(
            String transactionId,
            String creditDebitType,
            String transactionName,
            String type,
            BigDecimal amount,
            Instant transactionDateTime
    ) {}
}
