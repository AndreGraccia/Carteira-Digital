package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEvent.Kind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * "How do I get out of this hole?" Up to three actions for the first negative window, each one
 * tested by running the projection again. Nothing is suggested that the engine did not calculate.
 * <p>
 * The Wall.ia suggests; the user acts, in their own bank. Card invoices and loan instalments are never
 * suggested for postponing: paying them late costs interest and fines, which is the hole it wants to avoid.
 */
public final class ExitPlan {

    /** Expenses that can sometimes be moved by agreement (rent, school, bills), unlike invoices and loans. */
    static final Set<String> MOVABLE = Set.of("RENT", "BILL", "SUBSCRIPTION", "OTHER");

    private ExitPlan() {
    }

    /**
     * @param reserved money set aside in envelopes: the new daily pace must not eat into it, the same floor the
     *                 overview uses for its daily limit
     */
    public static List<Action> suggest(LocalDate today, BigDecimal openingBalance, List<CashEvent> events,
                                       int horizonDays, BigDecimal pace, BigDecimal reserved) {
        ProjectionEngine.Projection base = ProjectionEngine.project(today, openingBalance, events, horizonDays);
        if (base.riskWindows().isEmpty()) return List.of();
        ProjectionEngine.RiskWindow window = base.riskWindows().get(0);
        List<Action> actions = new ArrayList<>();

        // 1. Move one bill to the day the next income lands.
        if (window.relief() != null) {
            LocalDate relief = window.relief().date();
            window.causes().stream()
                    .filter(cause -> cause.kind() == Kind.RECURRING_EXPENSE && MOVABLE.contains(cause.category()))
                    .map(cause -> {
                        List<CashEvent> moved = new ArrayList<>(events);
                        moved.remove(cause);
                        moved.add(new CashEvent(relief, cause.amount(), cause.label(), cause.kind(), cause.category(),
                                cause.certainty(), cause.origin(), cause.reference()));
                        BigDecimal lowest = lowestUntil(today, openingBalance, moved, horizonDays, relief);
                        return new Action("POSTPONE", cause.label(), cause.category(), cause.amount().negate(),
                                cause.date(), relief, null, lowest, lowest.signum() >= 0);
                    })
                    .max(Comparator.comparing(Action::resultingLowest))
                    .ifPresent(actions::add);
        }

        // 2. Spend less per day until the next income, when bills alone fit.
        if (pace != null && pace.signum() > 0) {
            List<CashEvent> withoutDaily = events.stream().filter(event -> event.kind() != Kind.DAILY_SPENDING).toList();
            BigDecimal floor = reserved == null ? BigDecimal.ZERO : reserved;
            SpendingLimit.Limit limit = SpendingLimit.compute(today, openingBalance.subtract(floor), withoutDaily);
            if (limit.shortfall().signum() == 0 && pace.compareTo(limit.daily()) > 0) {
                // Run the projection again with the new pace until the income and the usual pace after it.
                List<CashEvent> cut = new ArrayList<>(withoutDaily);
                events.stream().filter(event -> event.kind() == Kind.DAILY_SPENDING)
                        .map(event -> event.date().isBefore(limit.until())
                                ? new CashEvent(event.date(), limit.daily().negate(), event.label(), event.kind(),
                                event.category(), event.certainty(), event.origin(), event.reference())
                                : event)
                        .forEach(cut::add);
                BigDecimal lowest = ProjectionEngine.project(today, openingBalance, cut, horizonDays).minBalance();
                actions.add(new Action("CUT_DAILY", null, "DAILY_SPENDING", pace.subtract(limit.daily()),
                        today.plusDays(1), limit.until(), limit.daily(), lowest, lowest.signum() >= 0));
            }
        }

        // 3. If nothing above closes it, say how much is missing and until when.
        if (actions.stream().noneMatch(Action::solves)) {
            actions.add(new Action("GAP", null, null, window.lowestBalance().negate(), window.start(),
                    window.lowestDate(), null, window.lowestBalance(), false));
        }
        return actions;
    }

    private static BigDecimal lowestUntil(LocalDate today, BigDecimal opening, List<CashEvent> events,
                                          int horizonDays, LocalDate until) {
        return ProjectionEngine.project(today, opening, events, horizonDays).days().stream()
                .filter(day -> !day.date().isAfter(until))
                .map(ProjectionEngine.DayPoint::balance)
                .min(Comparator.naturalOrder())
                .orElse(opening);
    }

    /**
     * @param type            POSTPONE (move a bill to the income date), CUT_DAILY (spend less per day) or GAP (missing money)
     * @param label           the bill to move (POSTPONE)
     * @param amount          bill value, cut per day, or missing amount
     * @param from            original due date, first day of the cut, or first negative day
     * @param to              new date, last day of the cut, or the lowest day
     * @param newDaily        daily spending after the cut (CUT_DAILY)
     * @param resultingLowest lowest balance until the next income after the action
     * @param solves          true when no day stays below zero after the action
     */
    public record Action(String type, String label, String category, BigDecimal amount, LocalDate from, LocalDate to,
                         BigDecimal newDaily, BigDecimal resultingLowest, boolean solves) {
    }
}
