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

    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InsightRateLimiter() {
        this(Clock.systemUTC());
    }

    InsightRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(Long userId) {
        Instant now = clock.instant();
        Window window = windows.compute(userId, (ignored, current) -> {
            if (current == null || current.startedAt().plus(1, ChronoUnit.HOURS).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (window.count() > MAX_REQUESTS_PER_HOUR) {
            throw new InsightRateLimitException();
        }
    }

    private record Window(Instant startedAt, int count) {
    }
}
