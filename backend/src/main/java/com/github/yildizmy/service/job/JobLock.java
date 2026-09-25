package com.github.yildizmy.service.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs a scheduled job on one instance only, with a PostgreSQL session advisory lock held on its own connection
 * for the whole run. With two replicas, the second one skips instead of syncing and e-mailing everyone twice.
 * The lock ends with the connection, so a crashed run never leaves it stuck.
 */
@Slf4j
@Component
public class JobLock {

    private final JdbcTemplate jdbc;

    public JobLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** For tests and single-process runs: the job always runs. */
    public static JobLock inProcess() {
        return new JobLock(null);
    }

    /** The job's result, or empty when another instance holds the lock. */
    public <T> Optional<T> runExclusive(String name, Supplier<T> job) {
        if (jdbc == null) return Optional.ofNullable(job.get());
        return jdbc.execute((ConnectionCallback<Optional<T>>) connection -> {
            if (!call(connection, "SELECT pg_try_advisory_lock(hashtext(?))", name)) {
                log.info("Job {} is running on another instance; skipped here", name);
                return Optional.empty();
            }
            try {
                return Optional.ofNullable(job.get());
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
