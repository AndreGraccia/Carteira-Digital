package com.github.yildizmy.dto.response;

import java.time.Instant;

public record OpenFinanceSyncResponse(
        String status,
        String institutionName,
        int accountsSynced,
        int newTransactions,
        Instant syncedAt
) {}
