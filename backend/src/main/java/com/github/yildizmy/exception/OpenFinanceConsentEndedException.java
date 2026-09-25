package com.github.yildizmy.exception;

/**
 * The institution says the consent is over (REJECTED in GET /consents/{consentId}): revoked in the bank's app,
 * expired or rejected there. Thrown by the gateway before any database work, so the caller can record it in a
 * transaction of its own (ERR-JAVA-016).
 */
public class OpenFinanceConsentEndedException extends RuntimeException {

    private final String rejectedBy;
    private final String reason;

    public OpenFinanceConsentEndedException(String rejectedBy, String reason) {
        super("Consent ended at the institution: " + rejectedBy + " / " + reason);
        this.rejectedBy = rejectedBy;
        this.reason = reason;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public String getReason() {
        return reason;
    }
}
