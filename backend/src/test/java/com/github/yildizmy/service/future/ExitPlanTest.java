package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.CashEvent.Certainty;
import com.github.yildizmy.service.future.CashEvent.Kind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExitPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Test
    void movingTheRentToPaydayClosesTheHoleAndTheInvoiceIsNeverSuggested() {
        // 1,000 now; rent 1,500 on day 3; card invoice 300 on day 4; salary 4,000 on day 6.
        List<CashEvent> events = List.of(
                event(3, "-1500.00", Kind.RECURRING_EXPENSE, "RENT", "Imobiliária"),
                event(4, "-300.00", Kind.CARD_INVOICE, "CARD_INVOICE", "Fatura"),
                event(6, "4000.00", Kind.INCOME, "SALARY", "Salário"));

        List<ExitPlan.Action> actions = ExitPlan.suggest(TODAY, new BigDecimal("1000.00"), events, 30, null);

        assertEquals(1, actions.size());
        ExitPlan.Action postpone = actions.get(0);
        assertEquals("POSTPONE", postpone.type());
        assertEquals("Imobiliária", postpone.label());
        assertEquals(TODAY.plusDays(3), postpone.from());
        assertEquals(TODAY.plusDays(6), postpone.to());
        // Without the rent before payday, the lowest point is 1,000 - 300.
        assertEquals(new BigDecimal("700.00"), postpone.resultingLowest());
        assertTrue(postpone.solves());
    }

    @Test
    void spendingLessPerDayIsSuggestedWhenTheBillsAloneFit() {
        // 1,000 now, rent 600 on day 2, salary on day 10, spending 60 a day: negative from day 7.
        List<CashEvent> events = new ArrayList<>(List.of(
                event(2, "-600.00", Kind.CARD_INVOICE, "CARD_INVOICE", "Fatura"),
                event(10, "3000.00", Kind.INCOME, "SALARY", "Salário")));
        events.addAll(CashEventPlanner.dailySpending(new BigDecimal("60.00"), TODAY, TODAY.plusDays(30), "média"));

        List<ExitPlan.Action> actions = ExitPlan.suggest(TODAY, new BigDecimal("1000.00"), events, 30, new BigDecimal("60.00"));

        ExitPlan.Action cut = actions.stream().filter(action -> action.type().equals("CUT_DAILY")).findFirst().orElseThrow();
        // The limit is 44.44 a day (400 over 9 days): 15.56 less than today's pace.
        assertEquals(new BigDecimal("15.56"), cut.amount());
        assertEquals(new BigDecimal("44.44"), cut.newDaily());
        assertEquals(TODAY.plusDays(10), cut.to());
        assertTrue(cut.solves());
    }

    @Test
    void withNothingToMoveItSaysHowMuchIsMissing() {
        List<CashEvent> events = List.of(
                event(2, "-500.00", Kind.LOAN_INSTALLMENT, "LOAN", "Empréstimo"),
                event(5, "2000.00", Kind.INCOME, "SALARY", "Salário"));

        List<ExitPlan.Action> actions = ExitPlan.suggest(TODAY, new BigDecimal("100.00"), events, 30, null);

        assertEquals(1, actions.size());
        assertEquals("GAP", actions.get(0).type());
        assertEquals(new BigDecimal("400.00"), actions.get(0).amount());
    }

    @Test
    void noHoleNoPlan() {
        assertTrue(ExitPlan.suggest(TODAY, new BigDecimal("100.00"), List.of(), 30, null).isEmpty());
    }

    private static CashEvent event(int day, String amount, Kind kind, String category, String label) {
        BigDecimal value = new BigDecimal(amount);
        return new CashEvent(TODAY.plusDays(day), value, label, kind, category,
                Certainty.CONFIRMED, "teste", label);
    }
}
