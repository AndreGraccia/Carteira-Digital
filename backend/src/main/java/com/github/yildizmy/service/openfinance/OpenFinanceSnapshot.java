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

    /**
     * @param completedAuthorisedPaymentType Accounts API: TRANSACAO_EFETIVADA (settled), LANCAMENTO_FUTURO
     *                                       (scheduled by the bank, the official data for projecting the
     *                                       balance) or TRANSACAO_PROCESSANDO; null is read as settled
     */
    public record Entry(
            String transactionId,
            String creditDebitType,
            String transactionName,
            String type,
            BigDecimal amount,
            Instant transactionDateTime,
            String completedAuthorisedPaymentType
    ) {
        public static final String SETTLED = "TRANSACAO_EFETIVADA";
        public static final String SCHEDULED = "LANCAMENTO_FUTURO";
        public static final String PROCESSING = "TRANSACAO_PROCESSANDO";

        public Entry(String transactionId, String creditDebitType, String transactionName, String type,
                     BigDecimal amount, Instant transactionDateTime) {
            this(transactionId, creditDebitType, transactionName, type, amount, transactionDateTime, SETTLED);
        }

        public boolean scheduled() {
            return SCHEDULED.equals(completedAuthorisedPaymentType);
        }

        public boolean processing() {
            return PROCESSING.equals(completedAuthorisedPaymentType);
        }
    }

    /**
     * What the import needs from the Credit Card Accounts API: the account, its total limit, the next bill
     * and the purchases with their instalments. {@code closingDay} is not in the API (bills carry only the due
     * date); the participant gateway estimates it.
     */
    public record CreditCardAccount(
            String creditCardAccountId,
            String name,
            String lastDigits,
            BigDecimal limitAmount,
            int closingDay,
            int dueDay,
            List<CardTransaction> transactions,
            Bill nextBill
    ) {
        public CreditCardAccount(String creditCardAccountId, String name, String lastDigits, BigDecimal limitAmount,
                                 int closingDay, int dueDay, List<CardTransaction> transactions) {
            this(creditCardAccountId, name, lastDigits, limitAmount, closingDay, dueDay, transactions, null);
        }
    }

    /** Bills API: {@code billMinimumAmount} is what keeps the card out of the revolving credit. */
    public record Bill(LocalDate dueDate, BigDecimal totalAmount, BigDecimal minimumAmount) {}

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
            String amortizationSystem,
            BigDecimal cet
    ) {
        public LoanContract(String contractId, String productName, BigDecimal contractAmount,
                            BigDecimal outstandingBalance, BigDecimal installmentAmount, int totalInstallments,
                            int paidInstallments, LocalDate nextDueDate, BigDecimal annualRate, Integer dueDay,
                            String amortizationSystem) {
            this(contractId, productName, contractAmount, outstandingBalance, installmentAmount, totalInstallments,
                    paidInstallments, nextDueDate, annualRate, dueDay, amortizationSystem, null);
        }
    }
}
