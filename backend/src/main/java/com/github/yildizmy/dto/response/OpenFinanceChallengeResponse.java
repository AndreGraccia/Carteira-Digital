package com.github.yildizmy.dto.response;

import java.time.Instant;

public record OpenFinanceChallengeResponse(
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt
) {}
