package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceGateway;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class OpenFinanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final OpenFinanceGateway gateway = mock(OpenFinanceGateway.class);
    private final OpenFinanceImportService importService = mock(OpenFinanceImportService.class);
    private final OpenFinanceConnectionRepository connections = mock(OpenFinanceConnectionRepository.class);
    private final boolean[] inTransaction = {false};
    private OpenFinanceConnection stored;
    private OpenFinanceService service;

    @BeforeEach
    void setUp() {
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setId(7L);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(gateway.institutionId()).thenReturn("bank-1");
        when(gateway.institutionName()).thenReturn("Banco Aurora Sandbox");
        when(connections.save(any())).thenAnswer(invocation -> stored = invocation.getArgument(0));
        when(connections.findFirstByUserIdOrderByIdDesc(7L)).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(connections.findByUserIdAndInstitutionId(7L, "bank-1")).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(gateway.fetch(7L)).thenAnswer(invocation -> {
            assertFalse(inTransaction[0], "the bank is never read inside a database transaction");
            return snapshot();
        });
        when(importService.importSnapshot(anyLong(), any(), any())).thenReturn(new OpenFinanceImportService.ImportResult(1, 3));
        service = new OpenFinanceService(gateway, importService, connections, mock(WalletRepository.class), users, clock,
                new TransactionOperations() {
                    // Behaves like TransactionTemplate: a RuntimeException rolls back what the callback wrote.
                    @Override
                    public <T> T execute(TransactionCallback<T> action) {
                        inTransaction[0] = true;
                        OpenFinanceConnection before = stored == null ? null : copy(stored);
                        int purgesBefore = committedPurges[0];
                        try {
                            return action.doInTransaction(null);
                        } catch (RuntimeException rollback) {
                            stored = before;
                            committedPurges[0] = purgesBefore;
                            throw rollback;
                        } finally {
                            inTransaction[0] = false;
                        }
                    }
                });
        doAnswer(invocation -> ++committedPurges[0]).when(importService).purge(7L);
    }

    private final int[] committedPurges = {0};

    private static OpenFinanceConnection copy(OpenFinanceConnection source) {
        OpenFinanceConnection copy = new OpenFinanceConnection();
        org.springframework.beans.BeanUtils.copyProperties(source, copy);
        return copy;
    }

    @Test
    void syncWithoutAConsentNeverReadsTheBank() {
        var error = assertThrows(CodedException.class, () -> service.sync(7L));

        assertEquals("OPEN_FINANCE_CONSENT_REQUIRED", error.getCode());
        verify(gateway, never()).fetch(anyLong());
    }

    @Test
    void consentAsksForEverythingTheImportReadsAndWaitsForAuthorisation() {
        var consent = service.createConsent(7L, null);

        assertEquals("AWAITING_AUTHORISATION", consent.status());
        assertTrue(consent.permissions().containsAll(List.of("RESOURCES_READ", "CREDIT_CARDS_ACCOUNTS_BILLS_READ",
                "LOANS_SCHEDULED_INSTALMENTS_READ")));
        assertEquals(Instant.parse("2027-09-21T12:00:00Z"), consent.expiresAt());
        assertEquals(NOW.plus(Duration.ofMinutes(60)), consent.authoriseBefore());
        assertTrue(consent.consentId().startsWith("urn:wallia:"));
        verify(gateway, never()).fetch(anyLong());

        var error = assertThrows(CodedException.class, () -> service.createConsent(7L, 5));
        assertEquals("OPEN_FINANCE_INVALID_DURATION", error.getCode());
    }

    @Test
    void authorisingImportsAndASyncNeverMovesTheExpiry() {
        var consent = service.createConsent(7L, 3);

        var result = service.authorise(7L, consent.consentId());

        assertEquals("AUTHORISED", result.status());
        assertEquals(OpenFinanceConnectionStatus.AUTHORISED, stored.getStatus());
        assertEquals(NOW, stored.getAuthorisedAt());
        Instant expiry = stored.getExpiresAt();

        clock.advance(Duration.ofDays(30));
        service.sync(7L);

        assertEquals(expiry, stored.getExpiresAt(), "a daily sync must not renew the consent");
        assertEquals(clock.instant(), stored.getLastSyncedAt());
        verify(importService, times(2)).importSnapshot(anyLong(), any(), any());
    }

    @Test
    void anAuthorisationAfterSixtyMinutesIsTooLate() {
        var consent = service.createConsent(7L, null);
        clock.advance(Duration.ofMinutes(61));

        var error = assertThrows(CodedException.class, () -> service.authorise(7L, consent.consentId()));

        assertEquals("OPEN_FINANCE_CONSENT_EXPIRED", error.getCode());
        assertEquals(OpenFinanceConnectionStatus.REJECTED, stored.getStatus());
        assertEquals("CONSENT_EXPIRED", stored.getRejectionReason());
        verify(gateway, never()).fetch(anyLong());
    }

    @Test
    void theCustomerCanSayNo() {
        var consent = service.createConsent(7L, null);

        service.reject(7L, consent.consentId());

        assertEquals(OpenFinanceConnectionStatus.REJECTED, stored.getStatus());
        assertEquals("USER", stored.getRejectedBy());
        assertEquals("CUSTOMER_MANUALLY_REJECTED", stored.getRejectionReason());
    }

    @Test
    void revokingRejectsTheConsentAndDeletesTheImportedData() {
        var consent = service.createConsent(7L, null);
        service.authorise(7L, consent.consentId());

        service.revoke(7L);

        assertEquals(OpenFinanceConnectionStatus.REJECTED, stored.getStatus());
        assertEquals("CUSTOMER_MANUALLY_REVOKED", stored.getRejectionReason());
        verify(importService).purge(7L);
        assertEquals("REJECTED", service.status(7L).status());
    }

    @Test
    void anExpiredConsentStopsSyncingAndIsRecordedAsSuch() {
        var consent = service.createConsent(7L, 3);
        service.authorise(7L, consent.consentId());
        clock.advance(Duration.ofDays(93));

        assertEquals("CONSENT_MAX_DATE_REACHED", service.status(7L).rejectionReason());
        var error = assertThrows(CodedException.class, () -> service.sync(7L));

        assertEquals("OPEN_FINANCE_CONSENT_REQUIRED", error.getCode());
        assertEquals(OpenFinanceConnectionStatus.REJECTED, stored.getStatus(), "the expiry survives the rollback of the 428");
        assertEquals("CONSENT_MAX_DATE_REACHED", stored.getRejectionReason());
        assertEquals(1, committedPurges[0], "data imported under an ended consent stops being used (LGPD, OFB)");
        verify(gateway, times(1)).fetch(7L);
    }

    @Test
    void aSecondConsentWhileConnectedIsAConflict() {
        service.authorise(7L, service.createConsent(7L, null).consentId());

        var error = assertThrows(CodedException.class, () -> service.createConsent(7L, null));

        assertEquals("OPEN_FINANCE_ALREADY_CONNECTED", error.getCode());
    }

    private OpenFinanceSnapshot snapshot() {
        var account = new OpenFinanceSnapshot.Account("acc", "Banco Aurora Sandbox", "CONTA_DEPOSITO_A_VISTA",
                "0001", "1", "0", BigDecimal.TEN, List.of());
        return new OpenFinanceSnapshot("bank-1", "Banco Aurora Sandbox", "c", NOW, null, List.of(account));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
