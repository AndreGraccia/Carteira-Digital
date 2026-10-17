package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEvent.Certainty;
import com.github.yildizmy.service.future.CashEvent.Kind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

    @Test
    void positiveMonthEndCanStillHideANegativeGapBeforePayday() {
        // Balance 1.000; rent 1.500 on day 1 and card 300 on day 3; salary 4.850 on day 5.
        var events = List.of(
                out(LocalDate.of(2026, 10, 1), "1500.00", "Imobiliária"),
                out(LocalDate.of(2026, 10, 3), "300.00", "Cartão"),
                in(LocalDate.of(2026, 10, 5), "4850.00", "Concentrix S.A.")
        );

        var projection = ProjectionEngine.project(TODAY, new BigDecimal("1000.00"), events, 30);

        assertEquals(new BigDecimal("4050.00"), projection.endBalance());
        assertEquals(new BigDecimal("-800.00"), projection.minBalance());
        assertEquals(LocalDate.of(2026, 10, 3), projection.minDate());
        assertEquals(4, projection.negativeDays());

        var window = projection.riskWindows().get(0);
        assertEquals(LocalDate.of(2026, 10, 1), window.start());
        assertEquals(LocalDate.of(2026, 10, 4), window.end());
        assertEquals(2, window.causes().size(), "both bills explain the gap");
        assertEquals("Concentrix S.A.", window.relief().label());
        assertFalse(window.untilHorizon());
    }

    @Test
    void noEventsKeepsTheBalanceFlatAndHasNoRisk() {
        var projection = ProjectionEngine.project(TODAY, new BigDecimal("250.00"), List.of(), 30);

        assertEquals(31, projection.days().size(), "today plus 30 days");
        assertEquals(new BigDecimal("250.00"), projection.minBalance());
        assertTrue(projection.riskWindows().isEmpty());
    }

    @Test
    void eventsOutsideTheHorizonAreIgnored() {
        var events = List.of(
                out(TODAY.minusDays(1), "999.00", "Ontem"),
                out(TODAY.plusDays(31), "999.00", "Depois do horizonte")
        );

        var projection = ProjectionEngine.project(TODAY, BigDecimal.TEN, events, 30);

        assertEquals(BigDecimal.TEN, projection.endBalance());
        assertTrue(projection.events().isEmpty());
    }

    @Test
    void windowStillOpenAtTheHorizonHasNoRelief() {
        var projection = ProjectionEngine.project(TODAY, BigDecimal.ZERO,
                List.of(out(TODAY.plusDays(10), "50.00", "Conta")), 30);

        var window = projection.riskWindows().get(0);
        assertTrue(window.untilHorizon());
        assertNull(window.relief());
    }

    @Test
    void separateNegativeStretchesBecomeSeparateWindows() {
        var events = List.of(
                out(TODAY.plusDays(2), "100.00", "A"),
                in(TODAY.plusDays(4), "200.00", "Entrada"),
                out(TODAY.plusDays(8), "200.00", "B"),
                in(TODAY.plusDays(9), "200.00", "Entrada 2")
        );

        var projection = ProjectionEngine.project(TODAY, BigDecimal.ZERO, events, 30);

        assertEquals(2, projection.riskWindows().size());
        assertEquals(List.of("B"), projection.riskWindows().get(1).causes().stream().map(CashEvent::label).toList(),
                "causes start at the last income, not at today");
    }

    static CashEvent out(LocalDate date, String amount, String label) {
        return new CashEvent(date, new BigDecimal(amount).negate(), label, Kind.RECURRING_EXPENSE, "BILL",
                Certainty.CONFIRMED, "teste", label);
    }

    static CashEvent in(LocalDate date, String amount, String label) {
        return new CashEvent(date, new BigDecimal(amount), label, Kind.INCOME, "SALARY",
                Certainty.ESTIMATED, "teste", label);
    }
}
