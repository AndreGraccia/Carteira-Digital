package com.github.yildizmy.service.future;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessDaysTest {

    @Test
    void easterAndTheMovableBankHolidays() {
        assertEquals(LocalDate.of(2026, 4, 5), BusinessDays.easter(2026));
        assertEquals(LocalDate.of(2027, 3, 28), BusinessDays.easter(2027));
        assertFalse(BusinessDays.isBusinessDay(LocalDate.of(2026, 2, 16)), "Carnaval (segunda)");
        assertFalse(BusinessDays.isBusinessDay(LocalDate.of(2026, 2, 17)), "Carnaval (terça)");
        assertFalse(BusinessDays.isBusinessDay(LocalDate.of(2026, 4, 3)), "Sexta-feira Santa");
        assertFalse(BusinessDays.isBusinessDay(LocalDate.of(2026, 6, 4)), "Corpus Christi");
        assertTrue(BusinessDays.isBusinessDay(LocalDate.of(2026, 2, 18)), "Quarta de cinzas abre");
    }

    @Test
    void billsMoveForwardAndIncomeMovesBack() {
        // 07/09/2026 is a Monday holiday; 05 and 06 are the weekend before it.
        assertEquals(LocalDate.of(2026, 9, 8), BusinessDays.onOrAfter(LocalDate.of(2026, 9, 5)));
        assertEquals(LocalDate.of(2026, 9, 4), BusinessDays.onOrBefore(LocalDate.of(2026, 9, 7)));
        assertFalse(BusinessDays.isBusinessDay(LocalDate.of(2026, 11, 20)), "Consciência Negra");
        assertEquals(LocalDate.of(2026, 9, 24), BusinessDays.onOrAfter(LocalDate.of(2026, 9, 24)));
    }

    @Test
    void salaryOnASundayIsProjectedOnTheFridayBeforeAndRentOnTheMondayAfter() {
        // 31/01/2027 is a Sunday.
        var movements = java.util.List.of(
                new RecurrenceDetector.Movement(1, LocalDate.of(2026, 11, 30), new java.math.BigDecimal("3000"),
                        RecurrenceDetector.Direction.IN, "Empresa", "Conta", "Banco"),
                new RecurrenceDetector.Movement(2, LocalDate.of(2026, 12, 31), new java.math.BigDecimal("3000"),
                        RecurrenceDetector.Direction.IN, "Empresa", "Conta", "Banco"),
                new RecurrenceDetector.Movement(3, LocalDate.of(2026, 11, 30), new java.math.BigDecimal("1500"),
                        RecurrenceDetector.Direction.OUT, "Imobiliária", "Conta", "Banco"),
                new RecurrenceDetector.Movement(4, LocalDate.of(2026, 12, 31), new java.math.BigDecimal("1500"),
                        RecurrenceDetector.Direction.OUT, "Imobiliária", "Conta", "Banco"));
        var patterns = RecurrenceDetector.detect(movements);
        LocalDate today = LocalDate.of(2027, 1, 10);

        var salary = CashEventPlanner.fromRecurrence(new CashEventPlanner.ConfirmedRecurrence(patterns.stream()
                .filter(p -> p.direction() == RecurrenceDetector.Direction.IN).findFirst().orElseThrow(),
                RecurrenceDetector.Category.SALARY), today, LocalDate.of(2027, 2, 5));
        var rent = CashEventPlanner.fromRecurrence(new CashEventPlanner.ConfirmedRecurrence(patterns.stream()
                .filter(p -> p.direction() == RecurrenceDetector.Direction.OUT).findFirst().orElseThrow(),
                RecurrenceDetector.Category.RENT), today, LocalDate.of(2027, 2, 5));

        assertEquals(LocalDate.of(2027, 1, 29), salary.get(0).date());
        assertEquals(LocalDate.of(2027, 2, 1), rent.get(0).date());
    }
}
