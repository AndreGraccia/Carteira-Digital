package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.dto.response.OpenFinanceChallengeResponse;
import com.github.yildizmy.dto.response.OpenFinanceStatusResponse;
import com.github.yildizmy.dto.response.OpenFinanceSyncResponse;
import com.github.yildizmy.exception.OpenFinanceVerificationException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceGateway;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OpenFinanceService {

    private static final List<String> PERMISSIONS = List.of(
            "ACCOUNTS_READ",
            "ACCOUNTS_BALANCES_READ",
            "ACCOUNTS_TRANSACTIONS_READ"
    );

    private final OpenFinanceGateway gateway;
    private final OpenFinanceImportService importService;
    private final OpenFinanceConnectionRepository connectionRepository;
    private final WalletRepository walletRepository;
    private final OpenFinanceEmailVerificationService verificationService;
    private final Clock clock;
    private final UserRepository userRepository;

    public OpenFinanceService(
            OpenFinanceGateway gateway,
            OpenFinanceImportService importService,
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            OpenFinanceEmailVerificationService verificationService,
            UserRepository userRepository,
            Clock clock) {
        this.gateway = gateway;
        this.importService = importService;
        this.connectionRepository = connectionRepository;
        this.walletRepository = walletRepository;
        this.verificationService = verificationService;
        this.clock = clock;
        this.userRepository = userRepository;
    }

    @Transactional
    public OpenFinanceChallengeResponse requestConnectionCode(long userId) {
        userRepository.findByIdForUpdate(userId);
        if (connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId)
                .filter(connection -> isActive(connection, clock.instant(), userId))
                .isPresent()) {
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_ALREADY_CONNECTED",
                    HttpStatus.CONFLICT
            );
        }
        return verificationService.request(userId);
    }

    @Transactional(noRollbackFor = OpenFinanceVerificationException.class)
    public OpenFinanceSyncResponse confirmConnection(long userId, String code) {
        userRepository.findByIdForUpdate(userId);
        if (connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId)
                .filter(connection -> isActive(connection, clock.instant(), userId))
                .isPresent()) {
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_ALREADY_CONNECTED",
                    HttpStatus.CONFLICT
            );
        }
        verificationService.verify(userId, code);
        return importFromGateway(userId);
    }

    @Transactional
    public OpenFinanceSyncResponse sync(long userId) {
        userRepository.findByIdForUpdate(userId);
        connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId)
                .filter(connection -> isActive(connection, clock.instant(), userId))
                .orElseThrow(() -> new OpenFinanceVerificationException(
                        "OPEN_FINANCE_VERIFICATION_REQUIRED",
                        HttpStatus.PRECONDITION_REQUIRED
                ));
        return importFromGateway(userId);
    }

    @Transactional(readOnly = true)
    public OpenFinanceStatusResponse status(long userId) {
        Instant now = clock.instant();
        var verification = verificationService.status(userId);
        var connection = connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId);
        long accounts = walletRepository.countByUserIdAndSource(userId, WalletSource.OPEN_FINANCE);

        return connection
                .map(item -> new OpenFinanceStatusResponse(
                        gateway.mode(),
                        gateway.simulated(),
                        statusOf(item, verification.pending(), now, userId),
                        item.getInstitutionName(),
                        item.getLastSyncedAt(),
                        item.getExpiresAt(),
                        accounts,
                        PERMISSIONS,
                        notice(),
                        verification
                ))
                .orElseGet(() -> new OpenFinanceStatusResponse(
                        gateway.mode(),
                        gateway.simulated(),
                        verification.pending() ? "PENDING_EMAIL_VERIFICATION" : "DISCONNECTED",
                        null,
                        null,
                        null,
                        accounts,
                        PERMISSIONS,
                        notice(),
                        verification
                ));
    }

    @Transactional
    public void revoke(long userId) {
        userRepository.findByIdForUpdate(userId);
        connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId).ifPresent(connection -> {
            connection.setStatus(OpenFinanceConnectionStatus.REVOKED);
            connectionRepository.save(connection);
        });
        verificationService.invalidate(userId);
    }

    private OpenFinanceSyncResponse importFromGateway(long userId) {
        var snapshot = gateway.fetch(userId);
        Instant syncedAt = clock.instant();
        var result = importService.importSnapshot(userId, snapshot, syncedAt);
        return new OpenFinanceSyncResponse(
                OpenFinanceConnectionStatus.AUTHORISED.name(),
                snapshot.institutionName(),
                result.accountsSynced(),
                result.newTransactions(),
                syncedAt
        );
    }

    private String statusOf(
            OpenFinanceConnection connection,
            boolean verificationPending,
            Instant now, long userId) {
        if (isActive(connection, now, userId)) return OpenFinanceConnectionStatus.AUTHORISED.name();
        if (verificationPending) return "PENDING_EMAIL_VERIFICATION";
        if (connection.getStatus() == OpenFinanceConnectionStatus.AUTHORISED) return "EXPIRED";
        return connection.getStatus().name();
    }

    private boolean isActive(OpenFinanceConnection connection, Instant now, long userId) {
        return connection.getStatus() == OpenFinanceConnectionStatus.AUTHORISED
                && verificationService.isVerified(userId)
                && (connection.getExpiresAt() == null || connection.getExpiresAt().isAfter(now));
    }

    private String notice() {
        return gateway.simulated()
                ? "Massa de contrato local alinhada à API oficial de Contas; não representa acesso ao Diretório oficial."
                : "Ambiente de sandbox da instituição participante configurada.";
    }
}
