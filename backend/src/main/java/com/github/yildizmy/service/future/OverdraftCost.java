package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * What a negative window would really cost on the overdraft (cheque especial): interest at the bank's rate
 * published by Banco Central, or the market median, never above the legal cap (Res. CMN 4.765/2019), plus IOF
 * for individuals (0.0082% a day on the balance used and 0.38% on the amount).
 * <p>
 * The engine already gives the interest at the cap, which is linear in the daily negative balance, so the
 * daily balance sum comes back from it without walking the days again.
 */
public final class OverdraftCost {

    public static final BigDecimal LEGAL_CAP_MONTHLY_PERCENT = new BigDecimal("8");
    static final BigDecimal IOF_DAILY = new BigDecimal("0.000082");
    static final BigDecimal IOF_FLAT = new BigDecimal("0.0038");

    public enum Source { BANK, MARKET, LEGAL_CAP }

    /** Monthly rate in percent, where it came from and the Banco Central reference date. */
    public record Rate(BigDecimal monthlyPercent, Source source, String institution, LocalDate referenceDate) {

        public static Rate legalCap() {
            return new Rate(LEGAL_CAP_MONTHLY_PERCENT, Source.LEGAL_CAP, null, null);
        }
    }

    public record Estimate(BigDecimal interest, BigDecimal iof, BigDecimal total, Rate rate) {
    }

    private OverdraftCost() {
    }

    /**
     * @param capInterest   interest of the window at the legal cap ({@link ProjectionEngine.RiskWindow#overdraftCostCap})
     * @param lowestBalance the lowest (negative) balance of the window
     * @param rate          the rate to use; null or above the cap falls back to the cap
     */
    public static Estimate estimate(BigDecimal capInterest, BigDecimal lowestBalance, Rate rate) {
        if (rate == null) rate = Rate.legalCap();
        if (rate.monthlyPercent().compareTo(LEGAL_CAP_MONTHLY_PERCENT) > 0) {
            rate = new Rate(LEGAL_CAP_MONTHLY_PERCENT, Source.LEGAL_CAP, rate.institution(), rate.referenceDate());
        }
        BigDecimal balanceDays = capInterest.divide(ProjectionEngine.OVERDRAFT_DAILY_RATE_CAP, MathContext.DECIMAL64);
        BigDecimal interest = balanceDays.multiply(daily(rate.monthlyPercent())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal iof = balanceDays.multiply(IOF_DAILY)
                .add(lowestBalance.abs().multiply(IOF_FLAT))
                .setScale(2, RoundingMode.HALF_UP);
        return new Estimate(interest, iof, interest.add(iof), rate);
    }

    /** Compound daily rate over 30 days, the same convention as the cap in the engine. */
    static BigDecimal daily(BigDecimal monthlyPercent) {
        double monthly = monthlyPercent.doubleValue() / 100;
        return BigDecimal.valueOf(Math.pow(1 + monthly, 1.0 / 30) - 1);
    }
}
