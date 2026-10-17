package com.github.yildizmy.service.future;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtPayoffTest {

    private static DebtPayoff.Debt debt(String id, String balance, String installment, String annualRate) {
        return new DebtPayoff.Debt(id, id, new BigDecimal(balance), new BigDecimal(installment), new BigDecimal(annualRate));
    }

    @Test
    void withoutInterestTheExtraOnlyShortensTheTime() {
        var comparison = DebtPayoff.compare(List.of(debt("a", "1000.00", "100.00", "0")), new BigDecimal("100.00"));

        assertEquals(10, comparison.baseline().months());
        assertEquals(5, comparison.avalanche().months());
        assertEquals(0, comparison.avalanche().interest().signum());
    }

    @Test
    void theBaselineKeepsPayingOnlyTheInstalmentsWhileThePlanRollsAFinishedOneOver() {
        // a ends in month 2; from month 3 its 50 goes to b: 800 left / 150 a month = 6 more months.
        var comparison = DebtPayoff.compare(
                List.of(debt("a", "100.00", "50.00", "0"), debt("b", "1000.00", "100.00", "0")), BigDecimal.ZERO);

        assertEquals(10, comparison.baseline().months());
        assertEquals(8, comparison.avalanche().months());
        assertEquals(8, comparison.snowball().months());
    }

    @Test
    void avalancheCostsLessAndSnowballClosesTheSmallDebtFirst() {
        var expensive = debt("cartao", "3000.00", "150.00", "100");
        var small = debt("pessoal", "600.00", "80.00", "20");

        var comparison = DebtPayoff.compare(List.of(small, expensive), new BigDecimal("200.00"));

        assertEquals("cartao", comparison.avalanche().order().get(0).id());
        assertEquals("pessoal", comparison.snowball().order().get(0).id());
        assertTrue(comparison.avalanche().interest().compareTo(comparison.snowball().interest()) < 0,
                "paying the highest rate first is the cheapest order");
        assertTrue(comparison.avalancheSaves().signum() > 0 && comparison.snowballSaves().signum() > 0);
        assertTrue(comparison.snowball().order().get(0).month() < comparison.avalanche().order().stream()
                .filter(payoff -> payoff.id().equals("pessoal")).findFirst().orElseThrow().month());
    }

    @Test
    void anInstalmentBelowTheInterestNeverEndsAndSaysSo() {
        // 100% a year is about 5.95% a month: 595.00 of interest against a 100.00 instalment.
        var comparison = DebtPayoff.compare(List.of(debt("a", "10000.00", "100.00", "100")), BigDecimal.ZERO);

        assertNull(comparison.baseline().months());
        assertNull(comparison.baseline().order().get(0).month());
    }

    @Test
    void theSameDebtTwiceIsTwoDebts() {
        var comparison = DebtPayoff.compare(
                List.of(debt("a", "500.00", "100.00", "0"), debt("a", "500.00", "100.00", "0")), BigDecimal.ZERO);

        assertEquals(5, comparison.baseline().months());
        assertEquals(2, comparison.baseline().order().size());
    }
}
