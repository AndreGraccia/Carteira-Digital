package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.WalletSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureBankDataAgeTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private static Wallet wallet(WalletSource source, String syncedAt) {
        Wallet wallet = new Wallet();
        wallet.setSource(source);
        wallet.setLastSyncedAt(syncedAt == null ? null : Instant.parse(syncedAt));
        return wallet;
    }

    @Test
    void theOldestBankAccountSaysHowOldTheBankDataIs() {
        List<Wallet> wallets = List.of(
                wallet(WalletSource.OPEN_FINANCE, "2026-09-25T10:30:00Z"),
                wallet(WalletSource.OPEN_FINANCE, "2026-09-24T10:30:00Z"),
                wallet(WalletSource.LOCAL, null));

        assertEquals(Instant.parse("2026-09-24T10:30:00Z"), FutureService.bankDataAsOf(wallets));
        assertFalse(FutureService.bankDataStale(FutureService.bankDataAsOf(wallets), NOW), "26 hours: one daily sync late at most");
    }

    @Test
    void moreThan36HoursMeansAtLeastOneDailySyncFailed() {
        assertTrue(FutureService.bankDataStale(Instant.parse("2026-09-23T23:59:00Z"), NOW));
        assertFalse(FutureService.bankDataStale(Instant.parse("2026-09-24T00:01:00Z"), NOW));
    }

    @Test
    void withoutBankAccountsThereIsNoBankDataToBeOld() {
        assertNull(FutureService.bankDataAsOf(List.of(wallet(WalletSource.LOCAL, null))));
        assertFalse(FutureService.bankDataStale(null, NOW));
    }
}
