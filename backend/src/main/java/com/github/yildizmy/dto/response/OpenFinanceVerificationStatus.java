package com.github.yildizmy.dto.response;

import java.time.Instant;

public record OpenFinanceVerificationStatus(
        boolean pending,
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt
) {
    public static OpenFinanceVerificationStatus none() {
        return new OpenFinanceVerificationStatus(false, null, null, null);
    }
}
