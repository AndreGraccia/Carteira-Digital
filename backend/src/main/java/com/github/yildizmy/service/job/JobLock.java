package com.github.yildizmy.service.job;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Runs a scheduled job on one instance only, with a PostgreSQL session advisory lock held on its own connection
 * for the whole run. With two replicas, the second one skips instead of syncing and e-mailing everyone twice.
 * The lock ends with the connection, so a crashed run never leaves it stuck.
 * <p>
 * Every job goes through here, so this is where they are measured (B36): {@code wallia.job} times each run by name
 * and outcome (success, failure, skipped), and {@code wallia.job.last.success} holds the epoch second of the last
 * success. An alert on "no success in 26 hours" catches a job that silently stopped.
 */
@Slf4j
@Component
public class JobLock {

    private final JdbcTemplate jdbc;
    private final MeterRegistry meters;
    private final Map<String, AtomicLong> lastSuccess = new ConcurrentHashMap<>();

    @Autowired
    public JobLock(JdbcTemplate jdbc, MeterRegistry meters) {
        this.jdbc = jdbc;
        this.meters = meters;
    }

    /** For tests and single-process runs: the job always runs. */
    public static JobLock inProcess() {
        return inProcess(new SimpleMeterRegistry());
    }

    public static JobLock inProcess(MeterRegistry meters) {
        return new JobLock(null, meters);
    }

    /** The job's result, or empty when another instance holds the lock. */
    public <T> Optional<T> runExclusive(String name, Supplier<T> job) {
        AtomicLong last = lastSuccess.computeIfAbsent(name, key ->
                meters.gauge("wallia.job.last.success", io.micrometer.core.instrument.Tags.of("name", key), new AtomicLong()));
        Timer.Sample sample = Timer.start(meters);
        String outcome = "failure";
        try {
            // Outer empty: another instance held the lock. Inner: the job's own result, which may be empty.
            Optional<Optional<T>> ran = jdbc == null ? Optional.of(Optional.ofNullable(job.get())) : locked(name, job);
            outcome = ran.isPresent() ? "success" : "skipped";
            if (ran.isPresent()) last.set(java.time.Instant.now().getEpochSecond());
            return ran.orElse(Optional.empty());
        } finally {
            sample.stop(meters.timer("wallia.job", "name", name, "outcome", outcome));
        }
    }

    private <T> Optional<Optional<T>> locked(String name, Supplier<T> job) {
        return jdbc.execute((ConnectionCallback<Optional<Optional<T>>>) connection -> {
            if (!call(connection, "SELECT pg_try_advisory_lock(hashtext(?))", name)) {
                log.info("Job {} is running on another instance; skipped here", name);
                return Optional.empty();
            }
            try {
                return Optional.of(Optional.ofNullable(job.get()));
            } finally {
                call(connection, "SELECT pg_advisory_unlock(hashtext(?))", name);
            }
        });
    }

    private static boolean call(java.sql.Connection connection, String sql, String name) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }
}
