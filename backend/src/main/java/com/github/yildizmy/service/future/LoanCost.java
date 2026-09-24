package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * What a loan still costs, in reais: the transparency investment platforms show about their own fees,
 * turned toward the person who owes. Two numbers, both from data the bank already sends.
 * <p>
 * Interest still to pay = instalment x instalments left - outstanding balance. Exact for any amortised
 * loan whose balance is the present value of what is left (Price or SAC), with no rate guess.
 * Interest inside the next instalment = outstanding balance x monthly rate equivalent to the annual one.
 */
public final class LoanCost {

    private LoanCost() {
    }

    /** Null when the bank did not send enough to say it honestly. */
    public static BigDecimal interestRemaining(BigDecimal installment, int remaining, BigDecimal outstanding) {
        if (installment == null || outstanding == null || remaining <= 0) return null;
        BigDecimal interest = installment.multiply(BigDecimal.valueOf(remaining)).subtract(outstanding);
        return interest.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /** @param annualRatePercent e.g. 29.84 for 29,84% a.a. */
    public static BigDecimal nextInstallmentInterest(BigDecimal outstanding, BigDecimal annualRatePercent, BigDecimal installment) {
        if (outstanding == null || annualRatePercent == null || annualRatePercent.signum() <= 0) return null;
        double monthly = Math.pow(1 + annualRatePercent.doubleValue() / 100, 1.0 / 12) - 1;
        BigDecimal interest = outstanding.multiply(new BigDecimal(monthly, MathContext.DECIMAL64));
        // Never more than the instalment itself: a mismatch in the bank data must not produce a nonsense split.
        if (installment != null) interest = interest.min(installment);
        return interest.setScale(2, RoundingMode.HALF_UP);
    }
}
