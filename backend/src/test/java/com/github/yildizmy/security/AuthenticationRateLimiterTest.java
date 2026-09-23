package com.github.yildizmy.security;

import com.github.yildizmy.exception.AuthenticationRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-23T12:00:00Z"));
    private final AuthenticationRateLimiter limiter = new AuthenticationRateLimiter(clock);

    @Test
    void parallelBurstGetsOnlyTheAddressBudget() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        for (int i = 0; i < 200; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    limiter.acquire("alice", "10.0.0.1");
                    allowed.incrementAndGet();
                } catch (AuthenticationRateLimitException ignored) {
                    // expected for everything past the budget
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(AuthenticationRateLimiter.MAX_PER_ADDRESS, allowed.get());
    }

    @Test
    void rotatingAddressesStopsAtTheUserBudget() {
        int allowed = 0;
        for (int i = 0; i < 100; i++) {
            try {
                limiter.acquire("Alice ", "10.0.0." + i);
                allowed++;
            } catch (AuthenticationRateLimitException ignored) {
                // user-wide limit reached
            }
        }
        assertEquals(AuthenticationRateLimiter.MAX_PER_USER, allowed);
    }

    @Test
    void successResetsAndWindowExpires() {
        for (int i = 0; i < AuthenticationRateLimiter.MAX_PER_ADDRESS; i++) limiter.acquire("bob", "ip");
        assertThrows(AuthenticationRateLimitException.class, () -> limiter.acquire("bob", "ip"));

        clock.now = clock.now.plus(AuthenticationRateLimiter.WINDOW).plusSeconds(1);
        assertDoesNotThrow(() -> limiter.acquire("bob", "ip"));

        for (int i = 1; i < AuthenticationRateLimiter.MAX_PER_ADDRESS; i++) limiter.acquire("bob", "ip");
        limiter.succeeded("bob", "ip");
        assertDoesNotThrow(() -> limiter.acquire("bob", "ip"));
    }

    @Test
    void otherUsersAreNotAffected() {
        for (int i = 0; i < AuthenticationRateLimiter.MAX_PER_ADDRESS; i++) limiter.acquire("carol", "ip");
        assertThrows(AuthenticationRateLimitException.class, () -> limiter.acquire("carol", "ip"));
        assertDoesNotThrow(() -> limiter.acquire("dave", "ip"));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
