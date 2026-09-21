package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.exception.OpenFinanceVerificationException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceGateway;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OpenFinanceServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private OpenFinanceGateway gateway;
    private OpenFinanceImportService importService;
    private OpenFinanceConnectionRepository connectionRepository;
    private OpenFinanceEmailVerificationService verificationService;
    private OpenFinanceService service;

    @BeforeEach
    void setUp() {
        gateway = mock(OpenFinanceGateway.class);
        importService = mock(OpenFinanceImportService.class);
        connectionRepository = mock(OpenFinanceConnectionRepository.class);
        verificationService = mock(OpenFinanceEmailVerificationService.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setId(7L);
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        service = new OpenFinanceService(gateway, importService, connectionRepository,
                mock(WalletRepository.class), verificationService, userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void syncRequiresConnectionAndNeverFetchesDataBeforeVerification() {
        when(connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(7L))
                .thenReturn(Optional.empty());

        var error = assertThrows(OpenFinanceVerificationException.class, () -> service.sync(7L));

        assertEquals("OPEN_FINANCE_VERIFICATION_REQUIRED", error.getCode());
        verifyNoInteractions(gateway, importService);
    }

    @Test
    void legacyAuthorisedConnectionCannotBypassEmailVerification() {
        connect(OpenFinanceConnectionStatus.AUTHORISED, NOW.plusSeconds(3600));
        when(verificationService.isVerified(7L)).thenReturn(false);

        var error = assertThrows(OpenFinanceVerificationException.class, () -> service.sync(7L));

        assertEquals("OPEN_FINANCE_VERIFICATION_REQUIRED", error.getCode());
        verifyNoInteractions(gateway, importService);
        service.requestConnectionCode(7L);
        verify(verificationService).request(7L);
    }

    @Test
    void invalidCodeNeverReachesGatewayOrImport() {
        doThrow(new OpenFinanceVerificationException("OPEN_FINANCE_CODE_INVALID", HttpStatus.BAD_REQUEST))
                .when(verificationService).verify(7L, "11111111");

        assertThrows(OpenFinanceVerificationException.class,
                () -> service.confirmConnection(7L, "11111111"));

        verifyNoInteractions(gateway, importService);
    }

    @Test
    void successfulConfirmationVerifiesCodeBeforeFetchingAndImportingUsersData() {
        OpenFinanceSnapshot snapshot = prepareImport();

        service.confirmConnection(7L, "87654321");

        var order = inOrder(verificationService, gateway, importService);
        order.verify(verificationService).verify(7L, "87654321");
        order.verify(gateway).fetch(7L);
        order.verify(importService).importSnapshot(7L, snapshot, NOW);
    }

    @Test
    void syncAcceptsActiveEmailVerifiedConnection() {
        connect(OpenFinanceConnectionStatus.AUTHORISED, NOW.plusSeconds(3600));
        when(verificationService.isVerified(7L)).thenReturn(true);
        OpenFinanceSnapshot snapshot = prepareImport();

        service.sync(7L);

        verify(importService).importSnapshot(7L, snapshot, NOW);
    }

    @Test
    void syncRejectsExpiredOrRevokedConnectionsEvenWhenEmailWasVerified() {
        OpenFinanceConnection connection = connect(OpenFinanceConnectionStatus.AUTHORISED, NOW);
        when(verificationService.isVerified(7L)).thenReturn(true);

        assertThrows(OpenFinanceVerificationException.class, () -> service.sync(7L));
        connection.setExpiresAt(NOW.plusSeconds(3600));
        connection.setStatus(OpenFinanceConnectionStatus.REVOKED);
        assertThrows(OpenFinanceVerificationException.class, () -> service.sync(7L));
        verifyNoInteractions(gateway, importService);
    }

    @Test
    void revokeInvalidatesVerificationAndPreventsFurtherSync() {
        OpenFinanceConnection connection = connect(OpenFinanceConnectionStatus.AUTHORISED, NOW.plusSeconds(3600));

        service.revoke(7L);

        assertEquals(OpenFinanceConnectionStatus.REVOKED, connection.getStatus());
        verify(connectionRepository).save(connection);
        verify(verificationService).invalidate(7L);
        assertThrows(OpenFinanceVerificationException.class, () -> service.sync(7L));
        verifyNoInteractions(gateway, importService);
    }

    private OpenFinanceConnection connect(OpenFinanceConnectionStatus status, Instant expiresAt) {
        OpenFinanceConnection connection = new OpenFinanceConnection();
        connection.setStatus(status);
        connection.setExpiresAt(expiresAt);
        when(connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(7L))
                .thenReturn(Optional.of(connection));
        return connection;
    }

    private OpenFinanceSnapshot prepareImport() {
        OpenFinanceSnapshot snapshot = new OpenFinanceSnapshot("sandbox", "Banco de teste", "consent-7",
                NOW, NOW.plusSeconds(3600), List.of());
        when(gateway.fetch(7L)).thenReturn(snapshot);
        when(importService.importSnapshot(7L, snapshot, NOW))
                .thenReturn(new OpenFinanceImportService.ImportResult(2, 15));
        return snapshot;
    }
}
