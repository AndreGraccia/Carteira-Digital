package com.github.yildizmy.service.openfinance;

import com.github.yildizmy.exception.CodedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Every consent as its own record, never overwritten (B42, V32): purpose, permissions, the text the person read
 * when authorising, and each state change as an appended event. The connection row only holds the current state.
 * Runs inside the caller's transaction, so an event exists exactly when the state change it describes committed.
 */
@Component
@RequiredArgsConstructor
public class OpenFinanceConsentRecord {

    /** The only purpose asked today; matches "openFinance.consent.purpose" on the screen. */
    public static final String PURPOSE = "FUTURE_PROJECTION";

    /**
     * Every consent text ever shown (frontend/src/consentTerms.json). An old version stays here: someone with an
     * old page open still authorised what that page said. A new text needs its version added here first.
     */
    static final Set<String> TERMS_VERSIONS = Set.of("2026-09-26");
    static final Set<String> LANGUAGES = Set.of("pt", "en");

    private final JdbcTemplate jdbc;

    public void created(long userId, String consentId, String institutionId, List<String> permissions,
                        Instant createdAt, Instant expiresAt) {
        jdbc.update("INSERT INTO open_finance_consent (consent_id, user_id, institution_id, purpose, created_at, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                consentId, userId, institutionId, PURPOSE, Timestamp.from(createdAt), Timestamp.from(expiresAt));
        jdbc.batchUpdate("INSERT INTO open_finance_consent_permission (consent_id, permission) VALUES (?, ?)",
                permissions.stream().map(permission -> new Object[]{consentId, permission}).toList());
        event(consentId, "CREATED", null, null, createdAt);
    }

    /** Checked before anything is written: an unknown text means we cannot say what the person agreed to. */
    public static void requireKnownTerms(String version, String language) {
        // Set.of(...).contains(null) throws, so a missing field is checked first.
        if (version == null || language == null || !TERMS_VERSIONS.contains(version) || !LANGUAGES.contains(language)) {
            throw new CodedException("OPEN_FINANCE_TERMS_UNKNOWN", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public void authorised(String consentId, String termsVersion, String language, Instant at) {
        jdbc.update("UPDATE open_finance_consent SET terms_version = ?, terms_language = ? WHERE consent_id = ?",
                termsVersion, language, consentId);
        event(consentId, "AUTHORISED", null, null, at);
    }

    public void rejected(String consentId, String actor, String reason, Instant at) {
        event(consentId, "REJECTED", actor, reason, at);
    }

    public List<String> permissions(String consentId) {
        return jdbc.queryForList("SELECT permission FROM open_finance_consent_permission WHERE consent_id = ? "
                + "ORDER BY permission", String.class, consentId);
    }

    private void event(String consentId, String event, String actor, String reason, Instant at) {
        jdbc.update("INSERT INTO open_finance_consent_event (consent_id, event, actor, reason, occurred_at) "
                + "VALUES (?, ?, ?, ?, ?)", consentId, event, actor, reason, Timestamp.from(at));
    }
}
