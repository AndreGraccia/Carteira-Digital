package com.github.yildizmy.dto.response;

import java.time.Instant;
import java.util.List;

public record OpenFinanceStatusResponse(
        String mode,
        boolean simulated,
        String status,
        String institutionName,
        Instant lastSyncedAt,
        Instant expiresAt,
        long accountCount,
        List<String> permissions,
        String notice
) {}
