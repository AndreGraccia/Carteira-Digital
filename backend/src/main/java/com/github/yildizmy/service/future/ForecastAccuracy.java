package com.github.yildizmy.service.future;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * "How right was Wall.ia?" Every synced day has a real balance. For each one, the forecast made most
 * recently between 7 and 30 days before is graded against it. Shorter leads are left out: guessing
 * tomorrow is easy and would flatter the score.
 * <p>
 * The difference includes what the user changed after seeing the forecast. That is the point: a negative
 * day that was predicted and did not happen is counted as avoided, which is the product's north star.
 * A negative day that happened without being predicted is counted too, as a miss, so the score cannot
 * only praise itself.
 */
public final class ForecastAccuracy {

    public static final int MIN_LEAD_DAYS = 7;
    public static final int MAX_LEAD_DAYS = 30;

    private ForecastAccuracy() {
    }

    /**
     * @param projection balances from {@code takenOn} (index 0) onwards; empty when nothing was confirmed yet
     */
    public record Snapshot(LocalDate takenOn, BigDecimal balance, List<BigDecimal> projection) {
    }

    /** {@code error} is actual minus projected: positive means the forecast was pessimistic. */
    public record Comparison(LocalDate date, LocalDate forecastOn, BigDecimal projected, BigDecimal actual, BigDecimal error) {
    }

    /**
     * @param meanAbsoluteError average size of the miss, in reais (null without comparisons)
     * @param bias              average signed miss: above zero the forecast tends to be pessimistic
     * @param avoided           days forecast below zero whose real balance was not
     * @param missed            days whose real balance was below zero without a negative forecast
     * @param firstComparisonOn when the first comparison becomes possible (null if there is no forecast yet)
     */
    public record Result(List<Comparison> comparisons, BigDecimal meanAbsoluteError, BigDecimal bias,
                         int avoided, int missed, LocalDate firstComparisonOn) {
    }

    /** @param snapshots one per day, oldest first */
    public static Result compare(List<Snapshot> snapshots) {
        List<Comparison> comparisons = new ArrayList<>();
        int avoided = 0;
        int missed = 0;
        for (Snapshot actual : snapshots) {
            Snapshot forecast = null;
            for (Snapshot candidate : snapshots) {
                long lead = ChronoUnit.DAYS.between(candidate.takenOn(), actual.takenOn());
                if (lead < MIN_LEAD_DAYS || lead > MAX_LEAD_DAYS || candidate.projection().size() < lead) continue;
                // The latest forecast in the window is the one the user was looking at.
                if (forecast == null || candidate.takenOn().isAfter(forecast.takenOn())) forecast = candidate;
            }
            if (forecast == null) continue;
            // projection[k] is the balance at the END of day k, and the sync that recorded the real balance runs in
            // the morning (7:30), before that day's bills: it is the end of the previous day. Comparing with
            // projection[k] would count every rent due on day D as "avoided".
            BigDecimal projected = forecast.projection().get((int) ChronoUnit.DAYS.between(forecast.takenOn(), actual.takenOn()) - 1);
            comparisons.add(new Comparison(actual.takenOn(), forecast.takenOn(), projected, actual.balance(),
                    actual.balance().subtract(projected)));
            if (projected.signum() < 0 && actual.balance().signum() >= 0) avoided++;
            if (projected.signum() >= 0 && actual.balance().signum() < 0) missed++;
        }
        LocalDate first = snapshots.stream()
                .filter(snapshot -> !snapshot.projection().isEmpty())
                .map(snapshot -> snapshot.takenOn().plusDays(MIN_LEAD_DAYS))
                .findFirst().orElse(null);
        if (comparisons.isEmpty()) return new Result(List.of(), null, null, 0, 0, first);
        BigDecimal count = BigDecimal.valueOf(comparisons.size());
        BigDecimal absolute = comparisons.stream().map(item -> item.error().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal signed = comparisons.stream().map(Comparison::error).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(comparisons, absolute.divide(count, 2, RoundingMode.HALF_UP),
                signed.divide(count, 2, RoundingMode.HALF_UP), avoided, missed, first);
    }
}
