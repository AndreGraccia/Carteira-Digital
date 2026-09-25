package com.github.yildizmy.service.future;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverdraftCostTest {

    // R$ 1.000 negative for 10 days: 10.000 "balance-days".
    private static final BigDecimal CAP_INTEREST = new BigDecimal("10000").multiply(ProjectionEngine.OVERDRAFT_DAILY_RATE_CAP)
            .setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal LOWEST = new BigDecimal("-1000.00");

    @Test
    void theEightPercentConventionMatchesTheEngineCap() {
        assertEquals(0, ProjectionEngine.OVERDRAFT_DAILY_RATE_CAP.compareTo(
                OverdraftCost.daily(OverdraftCost.LEGAL_CAP_MONTHLY_PERCENT).setScale(7, RoundingMode.HALF_UP)));
    }

    @Test
    void theBanksRateScalesTheInterestAndIofIsAdded() {
        OverdraftCost.Rate bank = new OverdraftCost.Rate(new BigDecimal("2.94"), OverdraftCost.Source.BANK,
                "BANCO ORIGINAL", LocalDate.of(2026, 9, 3));

        OverdraftCost.Estimate estimate = OverdraftCost.estimate(CAP_INTEREST, LOWEST, bank);

        // (1.0294^(1/30) - 1) × 10.000 = 9.66
        assertEquals(new BigDecimal("9.66"), estimate.interest());
        // 10.000 × 0,0082% + 1.000 × 0,38% = 0,82 + 3,80
        assertEquals(new BigDecimal("4.62"), estimate.iof());
        assertEquals(new BigDecimal("14.28"), estimate.total());
        assertEquals(OverdraftCost.Source.BANK, estimate.rate().source());
    }

    @Test
    void noRateOrARateAboveTheCapUsesTheCap() {
        assertEquals(CAP_INTEREST, OverdraftCost.estimate(CAP_INTEREST, LOWEST, null).interest());

        OverdraftCost.Estimate above = OverdraftCost.estimate(CAP_INTEREST, LOWEST,
                new OverdraftCost.Rate(new BigDecimal("12"), OverdraftCost.Source.MARKET, null, LocalDate.of(2026, 9, 3)));

        assertEquals(CAP_INTEREST, above.interest());
        assertEquals(OverdraftCost.Source.LEGAL_CAP, above.rate().source());
    }
}
