package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.dto.response.OpenFinanceConsentResponse;
import com.github.yildizmy.dto.response.OpenFinanceStatusResponse;
import com.github.yildizmy.dto.response.OpenFinanceSyncResponse;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceGateway;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The consent journey of Open Finance Brasil (Consents API v3), with the institution's authorisation screen
 * simulated by the sandbox (ADR 0008):
 * <pre>
 *   POST /connection            -> AWAITING_AUTHORISATION (60 minutes to decide)
 *   POST /connection/authorise  -> AUTHORISED, then the first import
 *   POST /connection/reject     -> REJECTED  (CUSTOMER_MANUALLY_REJECTED)
 *   DELETE /connection          -> REJECTED  (CUSTOMER_MANUALLY_REVOKED) and the imported data is deleted
 *   expirationDateTime reached  -> REJECTED  (CONSENT_MAX_DATE_REACHED), found on the next sync or status read
 * </pre>
 * The bank request never runs inside a database transaction or under the user's row lock: a slow institution
 * must not hold a connection from the pool.
 */
@Service
public class OpenFinanceService {

    /** What the import reads. RESOURCES_READ is mandatory in every Open Finance permission group. */
    static final List<String> PERMISSIONS = List.of(
            "ACCOUNTS_READ",
            "ACCOUNTS_BALANCES_READ",
            "ACCOUNTS_TRANSACTIONS_READ",
            "CREDIT_CARDS_ACCOUNTS_READ",
            "CREDIT_CARDS_ACCOUNTS_BILLS_READ",
            "CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ",
            "CREDIT_CARDS_ACCOUNTS_LIMITS_READ",
            "LOANS_READ",
            "LOANS_SCHEDULED_INSTALMENTS_READ",
            "LOANS_PAYMENTS_READ",
            "RESOURCES_READ"
    );
    static final Set<Integer> ALLOWED_MONTHS = Set.of(3, 6, 12);
    static final int DEFAULT_MONTHS = 12;
    /** Open Finance gives the customer 60 minutes to authorise a consent at the institution. */
    static final Duration AUTHORISATION_WINDOW = Duration.ofMinutes(60);

    private final OpenFinanceGateway gateway;
    private final OpenFinanceImportService importService;
    private final OpenFinanceConnectionRepository connectionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final TransactionOperations transactions;

    @Autowired
    public OpenFinanceService(
            OpenFinanceGateway gateway,
            OpenFinanceImportService importService,
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this(gateway, importService, connectionRepository, walletRepository, userRepository, clock,
                new TransactionTemplate(transactionManager));
    }

    OpenFinanceService(
            OpenFinanceGateway gateway,
            OpenFinanceImportService importService,
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository,
            Clock clock,
            TransactionOperations transactions) {
        this.gateway = gateway;
        this.importService = importService;
        this.connectionRepository = connectionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.transactions = transactions;
    }

    @Transactional
    public OpenFinanceConsentResponse createConsent(long userId, Integer months) {
        int duration = months == null ? DEFAULT_MONTHS : months;
        if (!ALLOWED_MONTHS.contains(duration)) {
            throw new CodedException("OPEN_FINANCE_INVALID_DURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        var user = userRepository.findByIdForUpdate(userId).orElseThrow();
        Instant now = clock.instant();
        OpenFinanceConnection connection = connectionRepository
                .findByUserIdAndInstitutionId(userId, gateway.institutionId())
                .orElseGet(OpenFinanceConnection::new);
        if (isActive(connection, now)) {
            throw new CodedException("OPEN_FINANCE_ALREADY_CONNECTED", HttpStatus.CONFLICT);
        }
        connection.setUser(user);
        connection.setInstitutionId(gateway.institutionId());
        connection.setInstitutionName(gateway.institutionName());
        connection.setConsentId("urn:wallia:" + UUID.randomUUID());
        connection.setStatus(OpenFinanceConnectionStatus.AWAITING_AUTHORISATION);
        connection.setCreatedAt(now);
        connection.setAuthorisedAt(null);
        connection.setExpiresAt(now.atZone(ZoneOffset.UTC).plusMonths(duration).toInstant());
        connection.setPermissions(String.join(",", PERMISSIONS));
        connection.setRejectedBy(null);
        connection.setRejectionReason(null);
        connection.setRejectedAt(null);
        connectionRepository.save(connection);
        return new OpenFinanceConsentResponse(connection.getConsentId(), connection.getStatus().name(),
                connection.getInstitutionName(), PERMISSIONS, now, connection.getExpiresAt(),
                now.plus(AUTHORISATION_WINDOW));
    }

    /** The person said yes on the institution's screen: authorise, then import for the first time. */
    public OpenFinanceSyncResponse authorise(long userId, String consentId) {
        transactions.executeWithoutResult(status -> expireIfDue(userId));
        transactions.executeWithoutResult(status -> {
            OpenFinanceConnection connection = awaiting(userId, consentId);
            connection.setStatus(OpenFinanceConnectionStatus.AUTHORISED);
            connection.setAuthorisedAt(clock.instant());
            connectionRepository.save(connection);
        });
        return sync(userId);
    }

    @Transactional(noRollbackFor = CodedException.class)
    public void reject(long userId, String consentId) {
        expireIfDue(userId);
        OpenFinanceConnection connection = awaiting(userId, consentId);
        connection.reject("USER", "CUSTOMER_MANUALLY_REJECTED", clock.instant());
        connectionRepository.save(connection);
    }

    /** Reads the institution outside any transaction, then imports in a short one. */
    public OpenFinanceSyncResponse sync(long userId) {
        transactions.executeWithoutResult(status -> expireIfDue(userId));
        transactions.executeWithoutResult(status -> activeConnection(userId));
        OpenFinanceSnapshot snapshot;
        try {
            snapshot = gateway.fetch(userId);
        } catch (com.github.yildizmy.exception.OpenFinanceConsentEndedException ended) {
            // Recorded and purged in a transaction that commits; the 428 comes after it (ERR-JAVA-016).
            transactions.executeWithoutResult(status -> endedAtTheInstitution(userId, ended));
            throw consentRequired();
        }
        return transactions.execute(status -> {
            userRepository.findByIdForUpdate(userId);
            // Revoked or expired while the bank was being read: drop what came back.
            OpenFinanceConnection connection = activeConnection(userId);
            Instant syncedAt = clock.instant();
            var result = importService.importSnapshot(userId, snapshot, syncedAt);
            connection.setLastSyncedAt(syncedAt);
            connection.setInstitutionName(snapshot.institutionName());
            connectionRepository.save(connection);
            return new OpenFinanceSyncResponse(
                    OpenFinanceConnectionStatus.AUTHORISED.name(),
                    snapshot.institutionName(),
                    result.accountsSynced(),
                    result.newTransactions(),
                    syncedAt
            );
        });
    }

    @Transactional(readOnly = true)
    public OpenFinanceStatusResponse status(long userId) {
        Instant now = clock.instant();
        long accounts = walletRepository.countByUserIdAndSource(userId, WalletSource.OPEN_FINANCE);
        return connectionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(connection -> {
                    // Reading never writes: an expired consent is shown as rejected and saved as such on the next sync.
                    String[] rejection = pendingRejection(connection, now);
                    String status = rejection != null ? OpenFinanceConnectionStatus.REJECTED.name() : connection.getStatus().name();
                    return new OpenFinanceStatusResponse(
                            gateway.mode(),
                            gateway.simulated(),
                            status,
                            connection.getConsentId(),
                            connection.getInstitutionName(),
                            connection.getCreatedAt(),
                            connection.getAuthorisedAt(),
                            connection.getLastSyncedAt(),
                            connection.getExpiresAt(),
                            rejection != null ? rejection[0] : connection.getRejectedBy(),
                            rejection != null ? rejection[1] : connection.getRejectionReason(),
                            accounts,
                            Arrays.asList(connection.getPermissions().split(",")),
                            notice());
                })
                .orElseGet(() -> new OpenFinanceStatusResponse(gateway.mode(), gateway.simulated(), "DISCONNECTED",
                        null, null, null, null, null, null, null, null, accounts, PERMISSIONS, notice()));
    }

    /** Revocation by the customer. The data imported under the consent is deleted with it. */
    @Transactional
    public void revoke(long userId) {
        userRepository.findByIdForUpdate(userId);
        connectionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .filter(connection -> connection.getStatus() != OpenFinanceConnectionStatus.REJECTED)
                .ifPresent(connection -> {
                    connection.reject("USER", "CUSTOMER_MANUALLY_REVOKED", clock.instant());
                    connectionRepository.save(connection);
                });
        importService.purge(userId);
    }

    private OpenFinanceConnection awaiting(long userId, String consentId) {
        userRepository.findByIdForUpdate(userId);
        OpenFinanceConnection connection = connectionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .filter(found -> found.getConsentId().equals(consentId))
                .filter(found -> found.getStatus() == OpenFinanceConnectionStatus.AWAITING_AUTHORISATION
                        || "CONSENT_EXPIRED".equals(found.getRejectionReason()))
                .orElseThrow(() -> new CodedException("OPEN_FINANCE_CONSENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (connection.getStatus() == OpenFinanceConnectionStatus.REJECTED
                && "CONSENT_EXPIRED".equals(connection.getRejectionReason())) {
            throw new CodedException("OPEN_FINANCE_CONSENT_EXPIRED", HttpStatus.GONE);
        }
        return connection;
    }

    /**
     * Records a consent that time alone has ended and deletes what was imported under it (LGPD art. 15 and 16;
     * the receiving institution stops using the data when the consent ends). Runs in its own transaction and never
     * throws: a write followed by a 428 in the same transaction was rolled back, so expiries were never saved.
     */
    private void expireIfDue(long userId) {
        Instant now = clock.instant();
        connectionRepository.findFirstByUserIdOrderByIdDesc(userId).ifPresent(connection -> {
            String[] rejection = pendingRejection(connection, now);
            if (rejection == null) return;
            connection.reject(rejection[0], rejection[1], now);
            connectionRepository.save(connection);
            importService.purge(userId);
        });
    }

    /** The institution reported the consent as REJECTED (revoked in its app, for example): same effect as ours. */
    private void endedAtTheInstitution(long userId, com.github.yildizmy.exception.OpenFinanceConsentEndedException ended) {
        connectionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .filter(connection -> connection.getStatus() == OpenFinanceConnectionStatus.AUTHORISED)
                .ifPresent(connection -> {
                    connection.reject(ended.getRejectedBy(), ended.getReason(), clock.instant());
                    connectionRepository.save(connection);
                });
        importService.purge(userId);
    }

    /** The authorised connection, or 428 asking to connect again. Only reads; {@link #expireIfDue} writes. */
    private OpenFinanceConnection activeConnection(long userId) {
        OpenFinanceConnection connection = connectionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(OpenFinanceService::consentRequired);
        if (!isActive(connection, clock.instant())) throw consentRequired();
        return connection;
    }

    private static CodedException consentRequired() {
        return new CodedException("OPEN_FINANCE_CONSENT_REQUIRED", HttpStatus.PRECONDITION_REQUIRED);
    }

    private boolean isActive(OpenFinanceConnection connection, Instant now) {
        return connection.getStatus() == OpenFinanceConnectionStatus.AUTHORISED && pendingRejection(connection, now) == null;
    }

    /** {rejectedBy, reason} when time alone has ended the consent, otherwise null. */
    private static String[] pendingRejection(OpenFinanceConnection connection, Instant now) {
        if (connection.getStatus() == OpenFinanceConnectionStatus.AUTHORISED
                && connection.getExpiresAt() != null && !now.isBefore(connection.getExpiresAt())) {
            return new String[]{"ASPSP", "CONSENT_MAX_DATE_REACHED"};
        }
        if (connection.getStatus() == OpenFinanceConnectionStatus.AWAITING_AUTHORISATION
                && !now.isBefore(connection.getCreatedAt().plus(AUTHORISATION_WINDOW))) {
            return new String[]{"ASPSP", "CONSENT_EXPIRED"};
        }
        return null;
    }

    private String notice() {
        return gateway.simulated() ? "CONTRACT_SANDBOX" : "PARTICIPANT_SANDBOX";
    }
}
