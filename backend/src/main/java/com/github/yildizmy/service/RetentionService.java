package com.github.yildizmy.service;

import com.github.yildizmy.service.job.JobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deletes what has outlived its purpose (LGPD art. 15 and 16), ADR 0015. Everything else lives while the account
 * does: Open Finance data goes with the consent, the rest with the account deletion (ADR 0012).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

    /** Rule name and the delete; each period is the ADR's, with the reason next to it. */
    static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        // The code is good for minutes; 30 days covers the resend window and support questions.
        RULES.put("email codes", "DELETE FROM email_verification_challenge WHERE created_at < now() - interval '30 days'");
        // The accuracy score reads up to 365 days back plus 30 days of lead (ForecastService, ForecastAccuracy).
        RULES.put("forecast snapshots", "DELETE FROM forecast_snapshot WHERE taken_on < current_date - 400");
        // Who did what as admin, kept one year for incident review.
        RULES.put("admin audit", "DELETE FROM admin_audit WHERE at < now() - interval '365 days'");
    }

    private final JdbcTemplate jdbc;
    private final JobLock jobLock;

    /** Early morning, before the market refresh and the sync; one instance runs it (advisory lock). */
    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}", zone = "America/Sao_Paulo")
    public void scheduled() {
        jobLock.runExclusive("data-retention", this::purge);
    }

    @Transactional
    public Map<String, Integer> purge() {
        Map<String, Integer> deleted = new LinkedHashMap<>();
        RULES.forEach((name, sql) -> deleted.put(name, jdbc.update(sql)));
        log.info("Retention: {}", deleted);
        return deleted;
    }
}
