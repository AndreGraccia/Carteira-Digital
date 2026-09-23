package com.github.yildizmy.service;

import com.github.yildizmy.dto.response.FutureResponses;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FutureVerdictTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Test
    void newNegativeDaysAreARisk() {
        assertEquals("RISK", FutureService.verdict(result(summary("500", 0), summary("-80", 3)), limit("30")));
    }

    @Test
    void lessThanAWeekOfEverydaySpendingLeftIsTight() {
        // 7 x 30 = 210 of buffer; the purchase leaves 150 at the lowest point.
        assertEquals("TIGHT", FutureService.verdict(result(summary("900", 0), summary("150", 0)), limit("30")));
    }

    @Test
    void deepeningAnExistingHoleIsARiskEvenWhenNegativeDaysCannotGrow() {
        // The whole horizon was already negative: days stay 91, the hole gets deeper.
        assertEquals("RISK", FutureService.verdict(result(summary("-50", 91), summary("-10050", 91)), limit(null)));
    }

    @Test
    void negativeDaysThatDoNotChangeKeepItTight() {
        assertEquals("TIGHT", FutureService.verdict(result(summary("-50", 2), summary("-50", 2)), limit(null)));
    }

    @Test
    void roomLeftIsSafe() {
        assertEquals("SAFE", FutureService.verdict(result(summary("900", 0), summary("400", 0)), limit("30")));
        assertEquals("SAFE", FutureService.verdict(result(summary("900", 0), summary("1", 0)), null));
    }

    private static FutureResponses.ScenarioResult result(FutureResponses.ScenarioSummary before,
                                                         FutureResponses.ScenarioSummary after) {
        return new FutureResponses.ScenarioResult(before, after, BigDecimal.ZERO, 1, null, null, null);
    }

    private static FutureResponses.ScenarioSummary summary(String min, long negativeDays) {
        BigDecimal value = new BigDecimal(min);
        return new FutureResponses.ScenarioSummary(value, value, DAY, negativeDays > 0 ? DAY : null, negativeDays, value);
    }

    private static FutureResponses.SpendingLimit limit(String pace) {
        return new FutureResponses.SpendingLimit(new BigDecimal("50"), DAY.plusDays(5), true, 5, DAY,
                pace == null ? null : new BigDecimal(pace), null, BigDecimal.ZERO);
    }
}
