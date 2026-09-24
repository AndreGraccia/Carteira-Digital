package com.github.yildizmy.service.openfinance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OpenFinanceSnapshot(
        String institutionId,
        String institutionName,
        String consentId,
        Instant authorisedAt,
        Instant expiresAt,
        List<Account> accounts,
        List<CreditCardAccount> creditCards,
        List<LoanContract> loans
) {

    /**
     * Accounts-only consent (the scope of the Accounts API). Cards and loans are null, meaning "not read",
     * so the import leaves them alone instead of treating them as paid off.
     */
    public OpenFinanceSnapshot(
            String institutionId,
            String institutionName,
            String consentId,
            Instant authorisedAt,
            Instant expiresAt,
            List<Account> accounts) {
        this(institutionId, institutionName, consentId, authorisedAt, expiresAt, accounts, null, null);
    }

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

    /** Shape inspired by the Credit Card Accounts API (accounts, bills and transactions). */
    public record CreditCardAccount(
            String creditCardAccountId,
            String name,
            String lastDigits,
            BigDecimal limitAmount,
            int closingDay,
            int dueDay,
            List<CardTransaction> transactions
    ) {}

    /**
     * @param firstInvoiceDue due month (any day) of the bill that carries instalment 1
     */
    public record CardTransaction(
            String transactionId,
            String transactionName,
            LocalDate purchaseDate,
            BigDecimal amount,
            int installments,
            LocalDate firstInvoiceDue
    ) {}

    /** Shape inspired by the Loans API (contract, balance and instalments). */
    public record LoanContract(
            String contractId,
            String productName,
            BigDecimal contractAmount,
            BigDecimal outstandingBalance,
            BigDecimal installmentAmount,
            int totalInstallments,
            int paidInstallments,
            LocalDate nextDueDate,
            BigDecimal annualRate,
            Integer dueDay,
            String amortizationSystem
    ) {}
}
