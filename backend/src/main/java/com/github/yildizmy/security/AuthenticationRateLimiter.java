package com.github.yildizmy.security;

import com.github.yildizmy.exception.AuthenticationRateLimitException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts login attempts before the password is checked, so parallel requests cannot all slip past
 * the limit while BCrypt is still running. A success clears the counters.
 * <p>
 * Two limits: per username and address (5), and per username from any address (20), so rotating
 * IPs does not reset the budget, while a shared proxy address cannot lock a user out with 5 tries.
 */
@Component
public class AuthenticationRateLimiter {
    static final int MAX_PER_ADDRESS = 5;
    static final int MAX_PER_USER = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    // ponytail: in-memory, one instance only. Move to Redis or the database when the API scales out.
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthenticationRateLimiter() {
        this(Clock.systemUTC());
    }

    AuthenticationRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Reserves one attempt or throws when a limit is already used up. */
    public void acquire(String username, String remoteAddress) {
        Instant now = clock.instant();
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(attempt -> attempt.expired(now));
        }
        String user = normalize(username);
        reserve(user + "|" + (remoteAddress == null ? "unknown" : remoteAddress), MAX_PER_ADDRESS, now);
        reserve(user + "|*", MAX_PER_USER, now);
    }

    public void succeeded(String username, String remoteAddress) {
        String user = normalize(username);
        attempts.remove(user + "|" + (remoteAddress == null ? "unknown" : remoteAddress));
        attempts.remove(user + "|*");
    }

    private void reserve(String key, int max, Instant now) {
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.expired(now)) return new Attempt(now, 1);
            // Throwing inside compute leaves the stored value untouched.
            if (current.count() >= max) throw new AuthenticationRateLimitException();
            return new Attempt(current.startedAt(), current.count() + 1);
        });
    }

    private static String normalize(String username) {
        return username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
    }

    private record Attempt(Instant startedAt, int count) {
        boolean expired(Instant now) {
            return startedAt.plus(WINDOW).isBefore(now);
        }
    }
}
