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

    public static final String DAILY_SPENDING = "DAILY_SPENDING";
    public static final String CARD_SPENDING = "CARD_SPENDING";
    public static final String GOAL = "GOAL";

    /** A monthly savings goal the user set: money that leaves the spendable balance on its day. */
    public record GoalPlan(long id, String name, BigDecimal monthlyAmount, int dayOfMonth) {
    }

    /** One CONFIRMED outflow per month for each goal, after today and until {@code end}. */
    public static List<CashEvent> goals(List<GoalPlan> goals, LocalDate today, LocalDate end) {
        List<CashEvent> events = new ArrayList<>();
        for (GoalPlan goal : goals) {
            for (YearMonth month = YearMonth.from(today); !month.atDay(1).isAfter(end); month = month.plusMonths(1)) {
                LocalDate date = month.atDay(Math.min(goal.dayOfMonth(), month.lengthOfMonth()));
                if (!date.isAfter(today) || date.isAfter(end)) continue;
                events.add(new CashEvent(date, goal.monthlyAmount().negate(), goal.name(), Kind.GOAL, GOAL,
                        Certainty.CONFIRMED, "Meta", "goal:" + goal.id()));
            }
        }
        return events;
    }

    /**
     * Everyday card purchases the user confirmed, from tomorrow on, grouped into the invoice each day
     * falls into and charged on that invoice's due date. Only invoices due inside the period appear.
     */
    public static List<CashEvent> cardSpending(BigDecimal daily, CardPlan card, LocalDate today, LocalDate end) {
        List<CashEvent> events = new ArrayList<>();
        if (daily == null || daily.signum() <= 0 || card == null) return events;
        java.util.Map<LocalDate, Integer> daysByDue = new java.util.TreeMap<>();
        for (LocalDate day = today.plusDays(1); !day.isAfter(end); day = day.plusDays(1)) {
            LocalDate due = BusinessDays.onOrAfter(card.dueDate(card.invoiceFor(day)));
            if (due.isAfter(end)) break;
            daysByDue.merge(due, 1, Integer::sum);
        }
        daysByDue.forEach((due, days) -> events.add(new CashEvent(
                due,
                daily.multiply(BigDecimal.valueOf(days)).negate(),
                "Compras do dia a dia no cartão",
                Kind.CARD_SPENDING,
                CARD_SPENDING,
                Certainty.ESTIMATED,
                card.name(),
                "card-spending:" + days)));
        return events;
    }

    /**
     * One estimated outflow per day, from tomorrow on, for the everyday spending the user confirmed.
     * Today is left out because the current balance already reflects what was spent today.
     */
    public static List<CashEvent> dailySpending(BigDecimal daily, LocalDate today, LocalDate end, String origin) {
        List<CashEvent> events = new ArrayList<>();
        if (daily == null || daily.signum() <= 0) return events;
        for (LocalDate day = today.plusDays(1); !day.isAfter(end); day = day.plusDays(1)) {
            events.add(new CashEvent(day, daily.negate(), "Gastos do dia a dia", Kind.DAILY_SPENDING,
                    DAILY_SPENDING, Certainty.ESTIMATED, origin, "daily-spending"));
        }
        return events;
    }

    static List<CashEvent> fromRecurrence(ConfirmedRecurrence recurrence, LocalDate today, LocalDate end) {
        RecurrencePattern pattern = recurrence.pattern();
        LocalDate lastSeen = pattern.occurrences().get(pattern.occurrences().size() - 1).date();
        List<CashEvent> events = new ArrayList<>();
        for (YearMonth month = YearMonth.from(today); !month.atDay(1).isAfter(end); month = month.plusMonths(1)) {
            LocalDate date = month.atDay(Math.min(pattern.typicalDay(), month.lengthOfMonth()));
            // Already happened: the balance reflects it. Decided by distance to the last occurrence, not by
            // calendar month, so a salary due on the 30th that landed on the 1st still counts for the next 30th.
            if (date.isBefore(today)
                    || java.time.temporal.ChronoUnit.DAYS.between(lastSeen, date) < RecurrenceDetector.MIN_GAP_DAYS) continue;
            if (date.isAfter(end)) break;
            boolean income = pattern.direction() == Direction.IN;
            // Closed bank day: income arrives the business day before, a bill is paid the business day after.
            LocalDate effective = income ? BusinessDays.onOrBefore(date) : BusinessDays.onOrAfter(date);
            if (effective.isBefore(today) || effective.isAfter(end)) continue;
            events.add(new CashEvent(
                    effective,
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
            LocalDate due = BusinessDays.onOrAfter(card.dueDate(invoice));
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
                .map(BusinessDays::onOrAfter)
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
