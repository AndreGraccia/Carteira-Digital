package com.github.yildizmy.service.ask;

import com.github.yildizmy.exception.AskRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guards and measures "Pergunte ao Wall.ia": a per-user request limit (each purchase question reads the
 * whole history and runs three projections) and a daily count per intent, with no text and no user.
 */
@Component
public class AskUsage {

    static final int MAX_PER_WINDOW = 30;
    static final Duration WINDOW = Duration.ofMinutes(10);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    // ponytail: in-memory, one instance only, like the other limiters. Move to Redis when the API scales out.
    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public AskUsage(JdbcTemplate jdbc) {
        this(jdbc, Clock.systemUTC());
    }

    AskUsage(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Reserves one question for the user or throws when the window is used up. */
    public void acquire(long userId) {
        Instant now = clock.instant();
        windows.compute(userId, (ignored, current) -> {
            if (current == null || current.start().plus(WINDOW).isBefore(now)) return new Window(now, 1);
            if (current.count() >= MAX_PER_WINDOW) throw new AskRateLimitException();
            return new Window(current.start(), current.count() + 1);
        });
    }

    public void record(String intent) {
        jdbc.update("""
                INSERT INTO ask_metric (day, intent, count) VALUES (?, ?, 1)
                ON CONFLICT (day, intent) DO UPDATE SET count = ask_metric.count + 1
                """, LocalDate.now(clock.withZone(ZONE)), intent);
    }

    /** Totals per intent over the last {@code days} days, largest first. */
    public List<IntentCount> summary(int days) {
        return jdbc.query("""
                        SELECT intent, SUM(count) AS total FROM ask_metric
                        WHERE day > ? GROUP BY intent ORDER BY total DESC, intent
                        """,
                (row, index) -> new IntentCount(row.getString("intent"), row.getLong("total")),
                LocalDate.now(clock.withZone(ZONE)).minusDays(days));
    }

    public record IntentCount(String intent, long count) {
    }

    private record Window(Instant start, int count) {
    }
}
