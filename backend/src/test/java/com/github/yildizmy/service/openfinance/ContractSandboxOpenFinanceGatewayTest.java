package com.github.yildizmy.service.openfinance;

import com.github.yildizmy.service.future.CardPlan;
import com.github.yildizmy.service.future.RecurrenceDetector;
import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sandbox is the demo customer: this locks the story it has to tell (salary, rent,
 * subscriptions, card instalments, a loan) and its internal consistency.
 */
class ContractSandboxOpenFinanceGatewayTest {

    private static final Instant NOW = Instant.parse("2026-09-22T15:00:00Z");
    private final OpenFinanceSnapshot snapshot =
            new ContractSandboxOpenFinanceGateway(Clock.fixed(NOW, ZoneOffset.UTC)).fetch(7L);

    @Test
    void historyProducesTheExpectedQuestions() {
        var movements = snapshot.accounts().stream()
                .flatMap(account -> account.transactions().stream().map(entry -> new RecurrenceDetector.Movement(
                        0, LocalDate.ofInstant(entry.transactionDateTime(), ContractSandboxOpenFinanceGateway.ZONE),
                        entry.amount(),
                        "CREDITO".equals(entry.creditDebitType()) ? Direction.IN : Direction.OUT,
                        entry.transactionName(), account.brandName(), account.brandName())))
                .toList();

        Map<String, Category> suggested = RecurrenceDetector.detect(movements).stream()
                .collect(Collectors.toMap(RecurrenceDetector.RecurrencePattern::key,
                        RecurrenceDetector.RecurrencePattern::suggestedCategory));

        assertEquals(Map.of(
                "in:concentrix-s-a", Category.SALARY,
                "in:marina-souza", Category.OTHER_INCOME,
                "out:imobiliaria-lar-seguro", Category.RENT,
                "out:netflix", Category.SUBSCRIPTION,
                "out:vivo-fibra", Category.SUBSCRIPTION,
                "out:smart-fit", Category.SUBSCRIPTION,
                "out:fluency-escola-de-ingles", Category.BILL,
                "out:enel-distribuicao-sp", Category.BILL
        ), suggested, "market, reserve transfers, card bill and loan must not become questions");
    }

    @Test
    void balanceIsTheReplayOfTheStatementAndNothingIsInTheFuture() {
        var checking = snapshot.accounts().get(0);
        BigDecimal replayed = checking.transactions().stream()
                .map(entry -> "CREDITO".equals(entry.creditDebitType()) ? entry.amount() : entry.amount().negate())
                .reduce(new BigDecimal("350.00"), BigDecimal::add);

        assertEquals(replayed, checking.availableBalance());
        assertTrue(snapshot.accounts().stream().flatMap(account -> account.transactions().stream())
                .allMatch(entry -> entry.transactionDateTime().isBefore(NOW)));
        assertTrue(snapshot.creditCards().get(0).transactions().stream()
                .allMatch(item -> item.purchaseDate().isBefore(LocalDate.of(2026, 9, 22))));
    }

    @Test
    void cardBillPaidFromCheckingMatchesTheInvoiceRebuiltFromCharges() {
        var cardAccount = snapshot.creditCards().get(0);
        var plan = new CardPlan(1, cardAccount.name(), "Banco", cardAccount.closingDay(), cardAccount.dueDay(),
                cardAccount.transactions().stream().map(item -> new CardPlan.Charge(0, item.transactionName(),
                        item.purchaseDate(), item.amount(),
                        item.amount().divide(BigDecimal.valueOf(item.installments()), 2, RoundingMode.DOWN),
                        item.installments(), YearMonth.from(item.firstInvoiceDue()))).toList());

        BigDecimal paidInSeptember = snapshot.accounts().get(0).transactions().stream()
                .filter(entry -> entry.transactionId().equals("card-bill-2026-09"))
                .map(OpenFinanceSnapshot.Entry::amount)
                .findFirst().orElseThrow();

        assertEquals(plan.invoiceTotal(YearMonth.of(2026, 9)), paidInSeptember);
        assertTrue(plan.invoiceTotal(YearMonth.of(2026, 10)).signum() > 0, "next bill already has instalments");
    }

    @Test
    void demoCustomerEndsTheMonthPositiveButDipsBelowZeroBeforePayday() {
        var patterns = RecurrenceDetector.detect(movements());
        var confirmed = patterns.stream()
                .map(pattern -> new com.github.yildizmy.service.future.CashEventPlanner.ConfirmedRecurrence(
                        pattern, pattern.suggestedCategory()))
                .toList();
        var loanSource = snapshot.loans().get(0);
        var loan = new com.github.yildizmy.service.future.LoanPlan(1, loanSource.productName(), "Banco",
                loanSource.outstandingBalance(), loanSource.installmentAmount(), loanSource.totalInstallments(),
                loanSource.paidInstallments(), loanSource.nextDueDate(), loanSource.dueDay());
        LocalDate today = LocalDate.of(2026, 9, 22);
        BigDecimal balance = snapshot.accounts().stream()
                .map(OpenFinanceSnapshot.Account::availableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var events = com.github.yildizmy.service.future.CashEventPlanner.plan(
                today, today.plusDays(30), confirmed, java.util.List.of(cardPlan()), java.util.List.of(loan));
        var projection = com.github.yildizmy.service.future.ProjectionEngine.project(today, balance, events, 30);

        assertTrue(projection.endBalance().signum() > 0, "positive at the end of the window: " + projection.endBalance());
        assertEquals(1, projection.riskWindows().size(), "balance " + balance + " min " + projection.minBalance());
        var window = projection.riskWindows().get(0);
        assertEquals(LocalDate.of(2026, 10, 2), window.start(), "card bill on the 2nd opens the gap");
        assertEquals(LocalDate.of(2026, 10, 4), window.end(), "salary on the 5th closes it");
        assertEquals("Concentrix S.A.", window.relief().label());
        assertEquals(new BigDecimal("-324.87"), window.lowestBalance());
    }

    private java.util.List<RecurrenceDetector.Movement> movements() {
        return snapshot.accounts().stream()
                .flatMap(account -> account.transactions().stream().map(entry -> new RecurrenceDetector.Movement(
                        0, LocalDate.ofInstant(entry.transactionDateTime(), ContractSandboxOpenFinanceGateway.ZONE),
                        entry.amount(),
                        "CREDITO".equals(entry.creditDebitType()) ? Direction.IN : Direction.OUT,
                        entry.transactionName(), account.brandName(), account.brandName())))
                .toList();
    }

    private CardPlan cardPlan() {
        var cardAccount = snapshot.creditCards().get(0);
        return new CardPlan(1, cardAccount.name(), "Banco", cardAccount.closingDay(), cardAccount.dueDay(),
                cardAccount.transactions().stream().map(item -> new CardPlan.Charge(0, item.transactionName(),
                        item.purchaseDate(), item.amount(),
                        item.amount().divide(BigDecimal.valueOf(item.installments()), 2, RoundingMode.DOWN),
                        item.installments(), YearMonth.from(item.firstInvoiceDue()))).toList());
    }

    @Test
    void loanReportsRemainingInstalmentsAndNextDueDate() {
        var loan = snapshot.loans().get(0);

        assertEquals(LocalDate.of(2026, 9, 25), loan.nextDueDate());
        assertEquals(9, loan.paidInstallments());
        assertTrue(loan.outstandingBalance().compareTo(loan.installmentAmount().multiply(BigDecimal.valueOf(15))) < 0,
                "outstanding principal is below the sum of remaining instalments");
        assertFalse(snapshot.creditCards().isEmpty());
    }
}
