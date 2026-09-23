package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Deterministic day-by-day balance projection.
 * <p>
 * No AI and no randomness: the same balance and events always produce the same result.
 * It answers two questions the month-end balance hides: what is the lowest point before
 * the next income, and which days go below zero (and because of what).
 */
public final class ProjectionEngine {

    private ProjectionEngine() {
    }

    public static Projection project(LocalDate today, BigDecimal openingBalance, List<CashEvent> events, int horizonDays) {
        LocalDate end = today.plusDays(horizonDays);
        List<CashEvent> inRange = events.stream()
                .filter(event -> !event.date().isBefore(today) && !event.date().isAfter(end))
                .sorted(Comparator.comparing(CashEvent::date).thenComparing(event -> event.amount().signum() < 0))
                .toList();

        Map<LocalDate, List<CashEvent>> byDay = new TreeMap<>();
        inRange.forEach(event -> byDay.computeIfAbsent(event.date(), day -> new ArrayList<>()).add(event));

        List<DayPoint> days = new ArrayList<>();
        BigDecimal balance = openingBalance;
        for (LocalDate day = today; !day.isAfter(end); day = day.plusDays(1)) {
            BigDecimal inflow = BigDecimal.ZERO;
            BigDecimal outflow = BigDecimal.ZERO;
            for (CashEvent event : byDay.getOrDefault(day, List.of())) {
                if (event.isInflow()) inflow = inflow.add(event.amount());
                else outflow = outflow.add(event.amount().negate());
            }
            balance = balance.add(inflow).subtract(outflow);
            days.add(new DayPoint(day, balance, inflow, outflow));
        }

        DayPoint lowest = days.stream().min(Comparator.comparing(DayPoint::balance)
                .thenComparing(DayPoint::date)).orElseThrow();
        return new Projection(
                today,
                openingBalance,
                days,
                lowest.balance(),
                lowest.date(),
                days.get(days.size() - 1).balance(),
                riskWindows(today, days, inRange),
                inRange
        );
    }

    /**
     * Groups consecutive negative days and explains each group with the outflows since the
     * last income (the "why") and the next income that ends it (the "relief").
     */
    private static List<RiskWindow> riskWindows(LocalDate today, List<DayPoint> days, List<CashEvent> events) {
        List<RiskWindow> windows = new ArrayList<>();
        int i = 0;
        while (i < days.size()) {
            if (days.get(i).balance().signum() >= 0) {
                i++;
                continue;
            }
            int start = i;
            DayPoint lowest = days.get(i);
            while (i < days.size() && days.get(i).balance().signum() < 0) {
                if (days.get(i).balance().compareTo(lowest.balance()) < 0) lowest = days.get(i);
                i++;
            }
            LocalDate startDate = days.get(start).date();
            LocalDate endDate = days.get(i - 1).date();
            LocalDate lowestDate = lowest.date();

            LocalDate lastIncome = events.stream()
                    .filter(CashEvent::isInflow)
                    .map(CashEvent::date)
                    .filter(date -> date.isBefore(startDate))
                    .max(LocalDate::compareTo)
                    .orElse(today);
            List<CashEvent> causes = events.stream()
                    .filter(event -> !event.isInflow())
                    .filter(event -> !event.date().isBefore(lastIncome) && !event.date().isAfter(lowestDate))
                    .toList();
            CashEvent relief = events.stream()
                    .filter(CashEvent::isInflow)
                    .filter(event -> event.date().isAfter(endDate))
                    .findFirst()
                    .orElse(null);
            boolean untilHorizon = i == days.size();
            windows.add(new RiskWindow(startDate, endDate, lowest.balance(), lowestDate, untilHorizon, causes, relief));
        }
        return windows;
    }

    public record DayPoint(LocalDate date, BigDecimal balance, BigDecimal inflow, BigDecimal outflow) {
    }

    /**
     * @param untilHorizon true when the balance is still negative on the last projected day
     * @param causes       outflows between the last income and the lowest day
     * @param relief       first income after the window, or null if none is expected in the horizon
     */
    public record RiskWindow(
            LocalDate start,
            LocalDate end,
            BigDecimal lowestBalance,
            LocalDate lowestDate,
            boolean untilHorizon,
            List<CashEvent> causes,
            CashEvent relief
    ) {
    }

    public record Projection(
            LocalDate start,
            BigDecimal openingBalance,
            List<DayPoint> days,
            BigDecimal minBalance,
            LocalDate minDate,
            BigDecimal endBalance,
            List<RiskWindow> riskWindows,
            List<CashEvent> events
    ) {

        public long negativeDays() {
            return days.stream().filter(day -> day.balance().signum() < 0).count();
        }
    }
}
