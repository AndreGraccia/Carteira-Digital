package com.github.yildizmy.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * @param status          DISCONNECTED, AWAITING_AUTHORISATION, AUTHORISED or REJECTED (Consents API v3)
 * @param rejectionReason when REJECTED: CUSTOMER_MANUALLY_REVOKED, CUSTOMER_MANUALLY_REJECTED, CONSENT_EXPIRED,
 *                        CONSENT_MAX_DATE_REACHED
 */
public record OpenFinanceStatusResponse(
        String mode,
        boolean simulated,
        String status,
        String consentId,
        String institutionName,
        Instant createdAt,
        Instant authorisedAt,
        Instant lastSyncedAt,
        Instant expiresAt,
        String rejectedBy,
        String rejectionReason,
        long accountCount,
        List<String> permissions,
        String notice
) {}
