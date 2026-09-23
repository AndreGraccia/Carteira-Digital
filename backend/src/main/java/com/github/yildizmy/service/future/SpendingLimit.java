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
 * largest daily amount {@code d} such that, for every day {@code t} (1 = tomorrow) before the next income,
 * {@code balance(t) - d * t >= 0}. Today is not a spending day: the balance already reflects it. The day that sets the limit is returned too, so the answer
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
        int spendingDays = Math.max((int) ChronoUnit.DAYS.between(today, until) - 1, 0);

        // Balance at the end of each day from today to the day before the income lands.
        ProjectionEngine.Projection projection =
                ProjectionEngine.project(today, openingBalance, fixedEvents, spendingDays);
        ProjectionEngine.DayPoint lowest = projection.days().get(0);
        BigDecimal limit = null;
        LocalDate binding = today;
        for (int t = 0; t < projection.days().size(); t++) {
            ProjectionEngine.DayPoint day = projection.days().get(t);
            if (day.balance().compareTo(lowest.balance()) < 0) lowest = day;
            if (t == 0) continue;
            BigDecimal candidate = day.balance().divide(BigDecimal.valueOf(t), 2, RoundingMode.DOWN);
            if (limit == null || candidate.compareTo(limit) < 0) {
                limit = candidate;
                binding = day.date();
            }
        }
        if (lowest.balance().signum() < 0) {
            // Bills alone do not fit: the useful number is how much is missing, and when.
            return new Limit(BigDecimal.ZERO, until, nextIncome != null, spendingDays, lowest.date(),
                    lowest.balance().negate());
        }
        BigDecimal daily = limit == null ? openingBalance.setScale(2, RoundingMode.DOWN) : limit;
        return new Limit(daily, until, nextIncome != null, spendingDays, binding, BigDecimal.ZERO);
    }

    /**
     * @param daily         largest daily spending that keeps every day at or above zero (zero if already short)
     * @param until         next income date, or the end of the default period
     * @param untilIncome   true when {@code until} is a confirmed income
     * @param days          spending days counted, from tomorrow to the day before {@code until}
     * @param bindingDate   the day that sets the limit (spending more than {@code daily} goes negative there first),
     *                      or, with a shortfall, the day the money runs out the most
     * @param shortfall     money missing before {@code until} even with no everyday spending at all (zero if none)
     */
    public record Limit(BigDecimal daily, LocalDate until, boolean untilIncome, int days, LocalDate bindingDate,
                        BigDecimal shortfall) {
    }
}
