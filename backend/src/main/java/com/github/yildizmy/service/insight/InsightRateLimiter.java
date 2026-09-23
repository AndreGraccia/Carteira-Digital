package com.github.yildizmy.service.insight;

import com.github.yildizmy.exception.InsightRateLimitException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InsightRateLimiter {
    private static final int MAX_REQUESTS_PER_HOUR = 8;
    // Caps the whole API key, whatever the number of accounts. Past it, reports fall back to local analysis.
    static final int MAX_GLOBAL_REQUESTS_PER_HOUR = 200;
    private static final Long GLOBAL_KEY = -1L;

    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InsightRateLimiter() {
        this(Clock.systemUTC());
    }

    InsightRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(Long userId) {
        if (count(userId) > MAX_REQUESTS_PER_HOUR) {
            throw new InsightRateLimitException();
        }
    }

    /** True while the shared Gemini budget for the current hour has room. */
    public boolean tryGlobal() {
        return count(GLOBAL_KEY) <= MAX_GLOBAL_REQUESTS_PER_HOUR;
    }

    private int count(Long key) {
        Instant now = clock.instant();
        return windows.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt().plus(1, ChronoUnit.HOURS).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        }).count();
    }

    private record Window(Instant startedAt, int count) {
    }
}
