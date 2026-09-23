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
            Event relief
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

    public record Overview(
            Instant generatedAt,
            LocalDate today,
            BigDecimal balance,
            BigDecimal freeAfterCommitments,
            Committed committed,
            Projection projection,
            Sources sources
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
