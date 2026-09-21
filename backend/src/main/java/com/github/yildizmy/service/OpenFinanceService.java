package com.github.yildizmy.service;

import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.dto.response.OpenFinanceStatusResponse;
import com.github.yildizmy.dto.response.OpenFinanceSyncResponse;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceGateway;
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
    private final Clock clock;

    public OpenFinanceService(
            OpenFinanceGateway gateway,
            OpenFinanceImportService importService,
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            Clock clock) {
        this.gateway = gateway;
        this.importService = importService;
        this.connectionRepository = connectionRepository;
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    public OpenFinanceSyncResponse sync(long userId) {
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

    @Transactional(readOnly = true)
    public OpenFinanceStatusResponse status(long userId) {
        var connection = connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId);
        long accounts = walletRepository.countByUserIdAndSource(userId, WalletSource.OPEN_FINANCE);
        return connection
                .map(item -> new OpenFinanceStatusResponse(
                        gateway.mode(), gateway.simulated(), item.getStatus().name(), item.getInstitutionName(),
                        item.getLastSyncedAt(), item.getExpiresAt(), accounts, PERMISSIONS, notice()))
                .orElseGet(() -> new OpenFinanceStatusResponse(
                        gateway.mode(), gateway.simulated(), "DISCONNECTED", null, null, null,
                        accounts, PERMISSIONS, notice()));
    }

    @Transactional
    public void revoke(long userId) {
        connectionRepository.findFirstByUserIdOrderByLastSyncedAtDesc(userId).ifPresent(connection -> {
            connection.setStatus(OpenFinanceConnectionStatus.REVOKED);
            connectionRepository.save(connection);
        });
    }

    private String notice() {
        return gateway.simulated()
                ? "Massa de contrato local alinhada à API oficial de Contas; não representa acesso ao Diretório oficial."
                : "Ambiente de sandbox da instituição participante configurada.";
    }
}
