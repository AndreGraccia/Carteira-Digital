package com.github.yildizmy.controller;

import com.github.yildizmy.exception.WalletRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** Read-only view of the admin trail. Reading it is itself an admin call, so it is audited too. */
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    static final int MAX_ROWS = 500;

    private final JdbcTemplate jdbc;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Entry>> recent(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 365) throw new WalletRuleException("INVALID_PERIOD");
        List<Entry> rows = jdbc.query("""
                        SELECT a.at, u.username, a.method, a.path, a.status
                        FROM admin_audit a JOIN "user" u ON u.id = a.actor_id
                        WHERE a.at > ? ORDER BY a.at DESC LIMIT ?
                        """,
                (row, index) -> new Entry(
                        row.getObject("at", OffsetDateTime.class).toInstant(),
                        row.getString("username"),
                        row.getString("method"),
                        row.getString("path"),
                        row.getInt("status")),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(days), MAX_ROWS);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rows);
    }

    public record Entry(Instant at, String username, String method, String path, int status) {
    }
}
