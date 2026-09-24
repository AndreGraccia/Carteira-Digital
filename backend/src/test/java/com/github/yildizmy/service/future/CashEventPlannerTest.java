package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEventPlanner.ConfirmedRecurrence;
import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.Movement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashEventPlannerTest {

    private long ids = 1;

    @Test
    void salaryThatSlippedIntoTheNextMonthStillHappensOnTheNextDueDay() {
        var salary = salary("2026-06-30", "2026-07-30", "2026-09-01");

        var dates = CashEventPlanner.fromRecurrence(salary, LocalDate.parse("2026-09-02"), LocalDate.parse("2026-10-31"))
                .stream().map(CashEvent::date).toList();

        assertEquals(List.of(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-30")), dates);
    }

    @Test
    void salaryPaidEarlyIsNotCountedAgainOnItsUsualDay() {
        var salary = salary("2026-07-30", "2026-08-30", "2026-09-28");

        var dates = CashEventPlanner.fromRecurrence(salary, LocalDate.parse("2026-09-29"), LocalDate.parse("2026-10-31"))
                .stream().map(CashEvent::date).toList();

        assertEquals(List.of(LocalDate.parse("2026-10-30")), dates);
    }

    @Test
    void dayThirtyOneBecomesTheLastDayOfShortMonths() {
        var rent = salary("2026-11-30", "2026-12-31", "2027-01-31");

        var dates = CashEventPlanner.fromRecurrence(rent, LocalDate.parse("2027-02-01"), LocalDate.parse("2027-04-30"))
                .stream().map(CashEvent::date).toList();

        // 28/02/2027 is a Sunday: income arrives on Friday 26/02.
        assertEquals(List.of(LocalDate.parse("2027-02-26"), LocalDate.parse("2027-03-31"), LocalDate.parse("2027-04-30")), dates);
    }

    @Test
    void typicalDayWrapsAroundTheTurnOfTheMonth() {
        assertEquals(30, RecurrenceDetector.detect(movements("2026-06-30", "2026-07-30", "2026-09-01")).get(0).typicalDay());
        assertEquals(1, RecurrenceDetector.detect(movements("2026-06-30", "2026-08-01", "2026-09-01")).get(0).typicalDay());
    }

    private ConfirmedRecurrence salary(String... dates) {
        return new ConfirmedRecurrence(RecurrenceDetector.detect(movements(dates)).get(0), Category.SALARY);
    }

    private List<Movement> movements(String... dates) {
        return java.util.Arrays.stream(dates)
                .map(date -> new Movement(ids++, LocalDate.parse(date), new BigDecimal("4200.00"), Direction.IN,
                        "Concentrix S.A.", "Conta", "Banco"))
                .toList();
    }
}
