package com.github.yildizmy.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Responses of the "Futuro" API. Every amount comes with the events that produced it,
 * so the client can always answer "why?".
 */
public final class FutureResponses {

    private FutureResponses() {
    }

    public record Event(
            LocalDate date,
            BigDecimal amount,
            String label,
            String kind,
            String category,
            String certainty,
            String origin,
            String reference
    ) {
    }

    public record DayPoint(LocalDate date, BigDecimal balance, BigDecimal inflow, BigDecimal outflow) {
    }

    public record RiskWindow(
            LocalDate start,
            LocalDate end,
            BigDecimal lowestBalance,
            LocalDate lowestDate,
            boolean untilHorizon,
            List<Event> causes,
            Event relief,
            BigDecimal overdraftCostCap
    ) {
    }

    public record Projection(
            int horizonDays,
            BigDecimal openingBalance,
            BigDecimal minBalance,
            LocalDate minDate,
            BigDecimal endBalance,
            long negativeDays,
            List<DayPoint> days,
            List<RiskWindow> riskWindows,
            List<Event> events
    ) {
    }

    public record CommittedGroup(String category, BigDecimal total, List<Event> items) {
    }

    /**
     * Money that already has a destination in the window: confirmed values and estimates are
     * reported separately and never merged.
     */
    public record Committed(
            LocalDate until,
            BigDecimal total,
            BigDecimal confirmed,
            BigDecimal estimated,
            List<CommittedGroup> groups
    ) {
    }

    public record Sources(int accounts, int cards, int loans, int confirmedRecurrences, int pendingQuestions) {
    }

    /**
     * Everyday spending (market, transport, pharmacy): a question like a recurrence.
     *
     * @param status        PENDING (not answered), CONFIRMED or REJECTED
     * @param suggestedDaily average per day found in the history, or null without enough history
     * @param appliedDaily  value the projection uses now (null unless CONFIRMED)
     * @param userDaily     value the user typed, when different from the suggestion
     * @param top           where most of the money went, as evidence for the question
     */
    public record DailySpending(
            String status,
            BigDecimal suggestedDaily,
            BigDecimal appliedDaily,
            BigDecimal userDaily,
            BigDecimal total,
            int days,
            int movements,
            List<SpendingItem> top
    ) {
    }

    public record SpendingItem(String counterparty, BigDecimal total, int count) {
    }

    /**
     * Answer to a free-text question. Only the fields of the detected intent are filled; every number
     * comes from the engine, and the client builds the sentence from them.
     *
     * @param intent  PURCHASE, DAILY_LIMIT, WHY_NEGATIVE, COMMITTED, NEXT_INCOME or UNKNOWN
     * @param verdict PURCHASE only: SAFE, TIGHT (no new negative day, but little room) or RISK (new negative days);
     *                NEED_AMOUNT when the question has no value
     */
    public record Answer(
            String intent,
            BigDecimal amount,
            int installments,
            String method,
            String verdict,
            BigDecimal balance,
            SpendingLimit spendingLimit,
            RiskWindow risk,
            Committed committed,
            Event nextIncome,
            ScenarioSummary before,
            ScenarioSummary after,
            BigDecimal newMonthlyCommitment,
            String cardName
    ) {
    }

    /**
     * How much can be spent per day until the next income without any day going below zero.
     *
     * @param bindingDate the day that sets the limit
     * @param pace        everyday spending the user confirmed, or null
     * @param margin      safeDaily minus pace: positive is room left, negative is how much to cut per day
     * @param shortfall   missing before {@code until} with bills alone, before any everyday spending (zero if none)
     */
    public record SpendingLimit(
            BigDecimal safeDaily,
            LocalDate until,
            boolean untilIncome,
            int days,
            LocalDate bindingDate,
            BigDecimal pace,
            BigDecimal margin,
            BigDecimal shortfall
    ) {
    }

    public record Overview(
            Instant generatedAt,
            LocalDate today,
            BigDecimal balance,
            BigDecimal freeAfterCommitments,
            Committed committed,
            Projection projection,
            Sources sources,
            DailySpending dailySpending,
            SpendingLimit spendingLimit
    ) {
    }

    public record Occurrence(long transactionId, LocalDate date, BigDecimal amount) {
    }

    /**
     * @param status PENDING (question not answered), CONFIRMED or REJECTED
     */
    public record Recurrence(
            String key,
            String counterparty,
            String direction,
            String accountName,
            String institution,
            BigDecimal typicalAmount,
            boolean fixedAmount,
            int typicalDay,
            String confidence,
            String suggestedCategory,
            String status,
            String category,
            Instant decidedAt,
            List<Occurrence> occurrences
    ) {
    }

    public record InvoiceLine(String description, LocalDate purchaseDate, int installment, int installments, BigDecimal amount) {
    }

    /**
     * @param status CLOSED (waiting for payment), OPEN (still receiving purchases) or FUTURE (instalments already booked)
     */
    public record Invoice(String month, LocalDate closingDate, LocalDate dueDate, String status, BigDecimal total, List<InvoiceLine> lines) {
    }

    public record Card(
            long id,
            String name,
            String institution,
            String lastDigits,
            BigDecimal creditLimit,
            BigDecimal committedLimit,
            int closingDay,
            int dueDay,
            List<Invoice> invoices
    ) {
    }

    public record Loan(
            long id,
            String product,
            String institution,
            BigDecimal contractedAmount,
            BigDecimal outstandingBalance,
            BigDecimal installmentAmount,
            int installmentsTotal,
            int installmentsPaid,
            int installmentsRemaining,
            LocalDate nextDueDate,
            LocalDate lastDueDate,
            BigDecimal annualRate,
            Instant lastSyncedAt
    ) {
    }

    public record Commitments(List<Card> cards, List<Loan> loans) {
    }

    public record ScenarioSummary(
            BigDecimal minBalance30,
            BigDecimal minBalance90,
            LocalDate minDate,
            LocalDate firstNegativeDate,
            long negativeDays,
            BigDecimal endBalance
    ) {
    }

    public record ScenarioResult(
            ScenarioSummary baseline,
            ScenarioSummary scenario,
            BigDecimal newMonthlyCommitment,
            int installments,
            String cardName,
            Projection baselineProjection,
            Projection scenarioProjection
    ) {
    }
}
