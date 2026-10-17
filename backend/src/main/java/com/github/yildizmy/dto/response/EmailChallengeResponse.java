package com.github.yildizmy.dto.response;

import java.time.Instant;

public record EmailChallengeResponse(
        String maskedEmail,
        Instant expiresAt,
        Instant resendAvailableAt
) {}
