package com.github.yildizmy.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * A consent waiting for the person to authorise it at the institution (here, the sandbox authorisation screen).
 *
 * @param authoriseBefore after this the consent is REJECTED with CONSENT_EXPIRED, as in Open Finance Brasil
 */
public record OpenFinanceConsentResponse(
        String consentId,
        String status,
        String institutionName,
        List<String> permissions,
        Instant createdAt,
        Instant expiresAt,
        Instant authoriseBefore
) {}
