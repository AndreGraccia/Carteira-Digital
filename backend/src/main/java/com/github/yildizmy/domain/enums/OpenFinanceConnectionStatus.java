package com.github.yildizmy.domain.enums;

/**
 * Consent status as in the Open Finance Brasil Consents API v3. There is no REVOKED: a revoked consent is
 * REJECTED with reason CUSTOMER_MANUALLY_REVOKED.
 */
public enum OpenFinanceConnectionStatus {
    AWAITING_AUTHORISATION,
    AUTHORISED,
    REJECTED
}
