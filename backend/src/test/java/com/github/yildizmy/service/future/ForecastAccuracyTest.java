package com.github.yildizmy.service.future;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ForecastAccuracyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 1);

    @Test
    void beforeSevenDaysThereIsNothingToGradeButTheDateOfTheFirstComparisonIsKnown() {
        ForecastAccuracy.Result result = ForecastAccuracy.compare(List.of(
                snapshot(0, "1000", flat("1000")),
                snapshot(3, "900", flat("900"))));

        assertEquals(List.of(), result.comparisons());
        assertNull(result.meanAbsoluteError());
        assertEquals(DAY.plusDays(7), result.firstComparisonOn());
    }

    @Test
    void theLatestForecastBetweenSevenAndThirtyDaysBeforeIsGraded() {
        List<BigDecimal> early = flat("500");
        List<BigDecimal> late = flat("800");
        ForecastAccuracy.Result result = ForecastAccuracy.compare(List.of(
                snapshot(0, "1000", early),
                snapshot(2, "1000", late),
                // Day 9: lead 9 from day 0 and 7 from day 2; day 2 wins. Its own forecast (lead 0) is ignored.
                snapshot(9, "750", flat("750"))));

        assertEquals(1, result.comparisons().size());
        ForecastAccuracy.Comparison comparison = result.comparisons().get(0);
        assertEquals(DAY.plusDays(2), comparison.forecastOn());
        assertEquals(new BigDecimal("800"), comparison.projected());
        assertEquals(new BigDecimal("-50"), comparison.error());
        assertEquals(new BigDecimal("50.00"), result.meanAbsoluteError());
        assertEquals(new BigDecimal("-50.00"), result.bias());
    }

    @Test
    void aPredictedHoleThatDidNotHappenIsAvoidedAndAnUnpredictedOneIsAMiss() {
        ForecastAccuracy.Result result = ForecastAccuracy.compare(List.of(
                snapshot(0, "300", flat("-100")),
                snapshot(1, "300", flat("200")),
                snapshot(7, "40", List.of()),
                snapshot(8, "-20", List.of())));

        assertEquals(1, result.avoided());
        assertEquals(1, result.missed());
    }

    @Test
    void aSnapshotWithoutConfirmedAnswersIsNeverUsedAsAForecast() {
        ForecastAccuracy.Result result = ForecastAccuracy.compare(List.of(
                snapshot(0, "1000", List.of()),
                snapshot(10, "900", List.of())));

        assertEquals(List.of(), result.comparisons());
        assertNull(result.firstComparisonOn());
    }

    private static ForecastAccuracy.Snapshot snapshot(int day, String balance, List<BigDecimal> projection) {
        return new ForecastAccuracy.Snapshot(DAY.plusDays(day), new BigDecimal(balance), projection);
    }

    private static List<BigDecimal> flat(String value) {
        List<BigDecimal> days = new ArrayList<>();
        for (int i = 0; i <= 30; i++) days.add(new BigDecimal(value));
        return days;
    }
}
