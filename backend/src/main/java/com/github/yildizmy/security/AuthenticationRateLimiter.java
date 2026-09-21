package com.github.yildizmy.security;

import com.github.yildizmy.exception.AuthenticationRateLimitException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthenticationRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    public void check(String username, String remoteAddress) {
        Attempt attempt = attempts.get(key(username, remoteAddress));
        if (attempt == null || attempt.startedAt().plus(WINDOW).isBefore(clock.instant())) return;
        if (attempt.failures() >= MAX_FAILURES) throw new AuthenticationRateLimitException();
    }

    public void failed(String username, String remoteAddress) {
        String key = key(username, remoteAddress);
        Instant now = clock.instant();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt().plus(WINDOW).isBefore(now)) return new Attempt(now, 1);
            return new Attempt(current.startedAt(), current.failures() + 1);
        });
    }

    public void succeeded(String username, String remoteAddress) {
        attempts.remove(key(username, remoteAddress));
    }

    private String key(String username, String remoteAddress) {
        String normalizedUsername = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        String normalizedAddress = remoteAddress == null ? "unknown" : remoteAddress;
        return normalizedUsername + "|" + normalizedAddress;
    }

    private record Attempt(Instant startedAt, int failures) {
    }
}
