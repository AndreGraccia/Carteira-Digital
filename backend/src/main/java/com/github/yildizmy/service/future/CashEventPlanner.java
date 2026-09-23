package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEvent.Certainty;
import com.github.yildizmy.service.future.CashEvent.Kind;
import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.RecurrencePattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns confirmed recurrences, card invoices and loans into dated {@link CashEvent}s.
 * Only facts the user confirmed or the institution reported become events.
 */
public final class CashEventPlanner {

    private CashEventPlanner() {
    }

    public static List<CashEvent> plan(
            LocalDate today,
            LocalDate end,
            List<ConfirmedRecurrence> recurrences,
            List<CardPlan> cards,
            List<LoanPlan> loans) {
        List<CashEvent> events = new ArrayList<>();
        recurrences.forEach(recurrence -> events.addAll(fromRecurrence(recurrence, today, end)));
        cards.forEach(card -> events.addAll(fromCard(card, today, end)));
        loans.forEach(loan -> events.addAll(fromLoan(loan, today, end)));
        return events;
    }

    static List<CashEvent> fromRecurrence(ConfirmedRecurrence recurrence, LocalDate today, LocalDate end) {
        RecurrencePattern pattern = recurrence.pattern();
        LocalDate lastSeen = pattern.occurrences().get(pattern.occurrences().size() - 1).date();
        List<CashEvent> events = new ArrayList<>();
        for (YearMonth month = YearMonth.from(today); !month.atDay(1).isAfter(end); month = month.plusMonths(1)) {
            LocalDate date = month.atDay(Math.min(pattern.typicalDay(), month.lengthOfMonth()));
            // Already happened this month (or earlier): the balance already reflects it.
            if (date.isBefore(today) || !YearMonth.from(date).isAfter(YearMonth.from(lastSeen))) continue;
            if (date.isAfter(end)) break;
            boolean income = pattern.direction() == Direction.IN;
            events.add(new CashEvent(
                    date,
                    income ? pattern.typicalAmount() : pattern.typicalAmount().negate(),
                    pattern.counterparty(),
                    income ? Kind.INCOME : Kind.RECURRING_EXPENSE,
                    recurrence.category().name(),
                    Certainty.ESTIMATED,
                    origin(pattern),
                    pattern.key()
            ));
        }
        return events;
    }

    static List<CashEvent> fromCard(CardPlan card, LocalDate today, LocalDate end) {
        List<CashEvent> events = new ArrayList<>();
        for (YearMonth invoice = YearMonth.from(today); !invoice.atDay(1).isAfter(end); invoice = invoice.plusMonths(1)) {
            LocalDate due = card.dueDate(invoice);
            if (due.isBefore(today) || due.isAfter(end)) continue;
            BigDecimal total = card.invoiceTotal(invoice);
            if (total.signum() == 0) continue;
            events.add(new CashEvent(
                    due,
                    total.negate(),
                    card.name(),
                    Kind.CARD_INVOICE,
                    "CARD_INVOICE",
                    Certainty.CONFIRMED,
                    card.institution(),
                    "card:" + card.id() + ":" + invoice
            ));
        }
        return events;
    }

    static List<CashEvent> fromLoan(LoanPlan loan, LocalDate today, LocalDate end) {
        return loan.dueDates().stream()
                .filter(date -> !date.isBefore(today) && !date.isAfter(end))
                .map(date -> new CashEvent(
                        date,
                        loan.installmentAmount().negate(),
                        loan.product(),
                        Kind.LOAN_INSTALLMENT,
                        "LOAN",
                        Certainty.CONFIRMED,
                        loan.institution(),
                        "loan:" + loan.id()
                ))
                .toList();
    }

    private static String origin(RecurrencePattern pattern) {
        if (pattern.institution() == null || pattern.institution().isBlank()) return pattern.accountName();
        if (pattern.accountName() == null) return pattern.institution();
        // Open Finance account names usually already carry the institution ("Banco X • 8392").
        return pattern.accountName().contains(pattern.institution())
                ? pattern.accountName()
                : pattern.institution() + " · " + pattern.accountName();
    }

    public record ConfirmedRecurrence(RecurrencePattern pattern, Category category) {
    }
}
