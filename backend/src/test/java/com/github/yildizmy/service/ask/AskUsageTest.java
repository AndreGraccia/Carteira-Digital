package com.github.yildizmy.service.ask;

import com.github.yildizmy.exception.AskRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AskUsageTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AskUsage usage = new AskUsage(jdbc, Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void eachUserHasItsOwnBudgetPerWindow() {
        for (int i = 0; i < AskUsage.MAX_PER_WINDOW; i++) usage.acquire(7L);

        assertThrows(AskRateLimitException.class, () -> usage.acquire(7L));
        assertDoesNotThrow(() -> usage.acquire(8L));
    }

    @Test
    void recordsOnlyTheIntentAndTheDay() {
        usage.record("PURCHASE");

        verify(jdbc).update(anyString(), eq(java.time.LocalDate.of(2026, 10, 1)), eq("PURCHASE"));
    }

    @Test
    void summaryAsksForTheRequestedPeriod() {
        usage.summary(30);

        verify(jdbc).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class),
                eq(java.time.LocalDate.of(2026, 9, 1)));
    }
}
