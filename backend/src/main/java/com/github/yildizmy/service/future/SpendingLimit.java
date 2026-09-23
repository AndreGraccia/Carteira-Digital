package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * "How much can I spend per day without going below zero before the next income?"
 * <p>
 * Runs the projection with bills, invoices and instalments only (no everyday spending) and finds the
 * largest daily amount {@code d} such that, for every day {@code t} of the period,
 * {@code balance(t) - d * (t + 1) >= 0}. The day that sets the limit is returned too, so the answer
 * can always be explained ("because of the rent on the 1st").
 */
public final class SpendingLimit {

    /** Without a known income, the limit covers the next 30 days. */
    static final int DEFAULT_DAYS = 30;
    /** An income further away than this is not "the next payday". */
    public static final int MAX_DAYS = 45;

    private SpendingLimit() {
    }

    /**
     * @param fixedEvents projection events without everyday spending
     */
    public static Limit compute(LocalDate today, BigDecimal openingBalance, List<CashEvent> fixedEvents) {
        LocalDate nextIncome = fixedEvents.stream()
                .filter(CashEvent::isInflow)
                .map(CashEvent::date)
                .filter(date -> date.isAfter(today) && !date.isAfter(today.plusDays(MAX_DAYS)))
                .min(Comparator.naturalOrder())
                .orElse(null);
        LocalDate until = nextIncome != null ? nextIncome : today.plusDays(DEFAULT_DAYS);
        int days = (int) ChronoUnit.DAYS.between(today, until);

        // Balance at the end of each day before the income lands; spending happens on each of those days.
        ProjectionEngine.Projection projection =
                ProjectionEngine.project(today, openingBalance, fixedEvents, Math.max(days - 1, 0));
        BigDecimal limit = null;
        LocalDate binding = today;
        BigDecimal lowest = openingBalance;
        for (int t = 0; t < projection.days().size(); t++) {
            ProjectionEngine.DayPoint day = projection.days().get(t);
            lowest = lowest.min(day.balance());
            BigDecimal candidate = day.balance().divide(BigDecimal.valueOf(t + 1L), 2, RoundingMode.DOWN);
            if (limit == null || candidate.compareTo(limit) < 0) {
                limit = candidate;
                binding = day.date();
            }
        }
        BigDecimal daily = limit == null ? BigDecimal.ZERO : limit.max(BigDecimal.ZERO);
        BigDecimal shortfall = lowest.signum() < 0 ? lowest.negate() : BigDecimal.ZERO;
        return new Limit(daily, until, nextIncome != null, days, binding, shortfall);
    }

    /**
     * @param daily         largest daily spending that keeps every day at or above zero (zero if already short)
     * @param until         next income date, or the end of the default period
     * @param untilIncome   true when {@code until} is a confirmed income
     * @param days          spending days counted, from today to the day before {@code until}
     * @param bindingDate   the day that sets the limit: spending more than {@code daily} goes negative there first
     * @param shortfall     money missing before {@code until} even with no everyday spending at all (zero if none)
     */
    public record Limit(BigDecimal daily, LocalDate until, boolean untilIncome, int days, LocalDate bindingDate,
                        BigDecimal shortfall) {
    }
}
