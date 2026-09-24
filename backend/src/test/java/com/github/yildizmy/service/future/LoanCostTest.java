package com.github.yildizmy.service.future;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoanCostTest {

    @Test
    void aPriceLoanSplitsIntoInterestAndPrincipalExactly() {
        // 12 instalments at 2,2% a month: the balance is the present value of the 12 payments.
        BigDecimal installment = new BigDecimal("612.48");
        double rate = 0.022;
        BigDecimal outstanding = installment
                .multiply(new BigDecimal((1 - Math.pow(1 + rate, -12)) / rate, MathContext.DECIMAL64))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal annual = new BigDecimal((Math.pow(1 + rate, 12) - 1) * 100, MathContext.DECIMAL64);

        assertEquals(installment.multiply(BigDecimal.valueOf(12)).subtract(outstanding),
                LoanCost.interestRemaining(installment, 12, outstanding));
        assertEquals(outstanding.multiply(new BigDecimal("0.022")).setScale(2, RoundingMode.HALF_UP),
                LoanCost.nextInstallmentInterest(outstanding, annual, installment));
    }

    @Test
    void missingOrInconsistentBankDataNeverProducesANonsenseNumber() {
        assertNull(LoanCost.interestRemaining(new BigDecimal("100"), 0, new BigDecimal("50")));
        assertNull(LoanCost.nextInstallmentInterest(new BigDecimal("1000"), null, new BigDecimal("100")));
        // Balance larger than all instalments left: no negative interest.
        assertEquals(new BigDecimal("0.00"), LoanCost.interestRemaining(new BigDecimal("100"), 2, new BigDecimal("500")));
        // A rate that would make the interest bigger than the instalment is capped at the instalment.
        assertEquals(new BigDecimal("100.00"), LoanCost.nextInstallmentInterest(new BigDecimal("100000"), new BigDecimal("30"), new BigDecimal("100")));
    }

    @Test
    void sacInterestFollowsTheFallingBalanceNotTheNextInstalment() {
        // 10.000 left, 10 instalments, 1% a month: interest = 0,01 x 10.000 x (10 + 1) / 2 = 550.
        BigDecimal annual = new BigDecimal((Math.pow(1.01, 12) - 1) * 100, MathContext.DECIMAL64);

        assertEquals(new BigDecimal("550.00"),
                LoanCost.interestRemaining("SAC", new BigDecimal("1100.00"), 10, new BigDecimal("10000.00"), annual));
        assertEquals(new BigDecimal("1000.00"),
                LoanCost.interestRemaining("PRICE", new BigDecimal("1100.00"), 10, new BigDecimal("10000.00"), annual));
        assertNull(LoanCost.interestRemaining("SAM", new BigDecimal("1100.00"), 10, new BigDecimal("10000.00"), annual),
                "an amortisation system the engine does not model gives no number");
        assertNull(LoanCost.interestRemaining("SAC", new BigDecimal("1100.00"), 10, new BigDecimal("10000.00"), null));
    }
}
