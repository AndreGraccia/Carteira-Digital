package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEvent.Certainty;
import com.github.yildizmy.service.future.CashEvent.Kind;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.Movement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailySpendingAndLimitTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private final AtomicLong ids = new AtomicLong();

    // --- everyday spending ----------------------------------------------------------------------

    @Test
    void averagesOnlyEverydayOutflowsOverTheWholePeriod() {
        List<Movement> movements = List.of(
                in("2026-07-03", "4200.00", "Concentrix S.A."),                // first movement: 90 days ago
                out("2026-07-10", "300.00", "Mercado Bairro"),
                out("2026-08-10", "250.00", "Mercado Bairro"),
                out("2026-09-10", "250.00", "Mercado Bairro"),
                out("2026-09-12", "100.00", "Farmácia Vida"),
                out("2026-09-01", "1500.00", "Imobiliária Lar Seguro"),       // fixed bill: projected on its own
                out("2026-09-03", "800.00", "Pagamento fatura Aurora Card"),  // card bill: comes from card data
                out("2026-09-06", "300.00", "Transferência para reserva"),    // own money going both ways
                in("2026-09-06", "300.00", "Transferência para reserva"),
                out("2026-10-01", "999.00", "Mercado Bairro"));               // today: not history yet

        DailySpending.Estimate estimate = DailySpending.estimate(TODAY, movements,
                Set.of("out:imobiliaria-lar-seguro")).orElseThrow();

        assertEquals(new BigDecimal("900.00"), estimate.total());
        assertEquals(90, estimate.days());
        assertEquals(new BigDecimal("10.00"), estimate.daily());
        assertEquals(4, estimate.movements());
        assertEquals("Mercado Bairro", estimate.top().get(0).counterparty());
        assertEquals(new BigDecimal("800.00"), estimate.top().get(0).total());
        assertEquals(3, estimate.top().get(0).count());
    }

    @Test
    void needsFourWeeksOfHistoryBeforeGuessing() {
        List<Movement> movements = List.of(
                out("2026-09-10", "50.00", "Mercado Bairro"),
                out("2026-09-20", "50.00", "Mercado Bairro"));

        assertTrue(DailySpending.estimate(TODAY, movements, Set.of()).isEmpty());
    }

    @Test
    void ignoresMovementsOlderThanTheWindow() {
        List<Movement> movements = List.of(
                out("2026-05-01", "5000.00", "Viagem"),
                in("2026-07-03", "4200.00", "Concentrix S.A."),
                out("2026-08-01", "90.00", "Mercado Bairro"));

        DailySpending.Estimate estimate = DailySpending.estimate(TODAY, movements, Set.of()).orElseThrow();

        assertEquals(new BigDecimal("90.00"), estimate.total());
        assertEquals(new BigDecimal("1.00"), estimate.daily());
    }

    // --- daily limit ----------------------------------------------------------------------------

    @Test
    void limitIsTheLargestDailyAmountThatKeepsEveryDayAtZeroOrAbove() {
        // 1,000 now, rent of 600 in two days, salary in ten days.
        List<CashEvent> events = List.of(
                event(TODAY.plusDays(2), "-600.00"),
                event(TODAY.plusDays(10), "3000.00"));

        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("1000.00"), events);

        // 400 left after rent, spread over the 9 spending days (tomorrow to the day before the salary).
        assertEquals(new BigDecimal("44.44"), limit.daily());
        assertEquals(TODAY.plusDays(10), limit.until());
        assertTrue(limit.untilIncome());
        assertEquals(9, limit.days());
        assertEquals(TODAY.plusDays(9), limit.bindingDate());
        assertEquals(0, limit.shortfall().signum());
    }

    @Test
    void theShortfallPointsToTheDayTheMoneyRunsOutTheMost() {
        // -900 on day 2, -1,000 on day 20: the card must say "1,000 short" and point to day 20.
        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("100.00"), List.of(
                event(TODAY.plusDays(2), "-1000.00"),
                event(TODAY.plusDays(20), "-100.00"),
                event(TODAY.plusDays(25), "4000.00")));

        assertEquals(new BigDecimal("1000.00"), limit.shortfall());
        assertEquals(TODAY.plusDays(20), limit.bindingDate());
    }

    @Test
    void whenBillsAloneGoBelowZeroTheLimitIsZeroAndTheShortfallIsKnown() {
        // 300 now, 700 due in two days, salary in five: 400 missing even without spending anything.
        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("300.00"),
                List.of(event(TODAY.plusDays(2), "-700.00"), event(TODAY.plusDays(5), "4000.00")));

        assertEquals(0, limit.daily().signum());
        assertEquals(new BigDecimal("400.00"), limit.shortfall());
        assertEquals(TODAY.plusDays(2), limit.bindingDate());
    }

    @Test
    void anEarlyBillCanSetTheLimitBeforeTheEndOfTheCycle() {
        // 500 now, 450 due tomorrow, next salary only in 20 days: tomorrow sets the pace.
        List<CashEvent> events = List.of(
                event(TODAY.plusDays(1), "-450.00"),
                event(TODAY.plusDays(20), "3000.00"),
                event(TODAY.plusDays(3), "-10.00"));

        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("500.00"), events);

        // After tomorrow 50 are left and then 10 more leave: 40 for 19 spending days.
        assertEquals(new BigDecimal("2.10"), limit.daily());
        assertEquals(TODAY.plusDays(19), limit.bindingDate());
    }

    @Test
    void alreadyShortMeansZeroNotNegative() {
        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("-50.00"),
                List.of(event(TODAY.plusDays(5), "2000.00")));

        assertEquals(0, limit.daily().signum());
        assertEquals(new BigDecimal("50.00"), limit.shortfall());
        assertEquals(TODAY, limit.bindingDate());
    }

    @Test
    void withoutIncomeTheLimitCoversThirtyDays() {
        SpendingLimit.Limit limit = SpendingLimit.compute(TODAY, new BigDecimal("300.00"), List.of());

        assertFalse(limit.untilIncome());
        assertEquals(TODAY.plusDays(30), limit.until());
        assertEquals(new BigDecimal("10.34"), limit.daily());
    }

    @Test
    void confirmedEverydaySpendingBecomesOneEstimatedEventPerDay() {
        List<CashEvent> events = CashEventPlanner.dailySpending(new BigDecimal("25.00"), TODAY, TODAY.plusDays(2), "média");

        // Tomorrow and the day after: today's spending is already in the balance.
        assertEquals(2, events.size());
        assertEquals(TODAY.plusDays(1), events.get(0).date());
        assertTrue(events.stream().allMatch(item -> item.certainty() == Certainty.ESTIMATED
                && item.kind() == Kind.DAILY_SPENDING
                && item.amount().compareTo(new BigDecimal("-25.00")) == 0));
        assertTrue(CashEventPlanner.dailySpending(null, TODAY, TODAY.plusDays(2), "média").isEmpty());
    }

    // --- cost of doing nothing ------------------------------------------------------------------

    @Test
    void riskWindowCarriesTheOverdraftInterestAtTheLegalCap() {
        // 100 now, 200 leave tomorrow, 500 arrive in three days: two days at -100.
        ProjectionEngine.Projection projection = ProjectionEngine.project(TODAY, new BigDecimal("100.00"),
                List.of(event(TODAY.plusDays(1), "-200.00"), event(TODAY.plusDays(3), "500.00")), 5);

        ProjectionEngine.RiskWindow window = projection.riskWindows().get(0);

        // 2 days x 100 x 0.0025687 (8% a month, compounded daily) = 0.51
        assertEquals(new BigDecimal("0.51"), window.overdraftCostCap());
    }

    private Movement out(String date, String amount, String counterparty) {
        return new Movement(ids.incrementAndGet(), LocalDate.parse(date), new BigDecimal(amount), Direction.OUT,
                counterparty, "Conta corrente", "Banco Aurora Sandbox");
    }

    private Movement in(String date, String amount, String counterparty) {
        return new Movement(ids.incrementAndGet(), LocalDate.parse(date), new BigDecimal(amount), Direction.IN,
                counterparty, "Conta corrente", "Banco Aurora Sandbox");
    }

    private static CashEvent event(LocalDate date, String amount) {
        BigDecimal value = new BigDecimal(amount);
        return new CashEvent(date, value, value.signum() > 0 ? "Salário" : "Conta",
                value.signum() > 0 ? Kind.INCOME : Kind.RECURRING_EXPENSE,
                value.signum() > 0 ? "SALARY" : "BILL", Certainty.CONFIRMED, "teste", "ref");
    }
}
