package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.domain.enums.Status;
import com.github.yildizmy.domain.enums.TransactionSource;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class OpenFinanceImportService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final OpenFinanceConnectionRepository connectionRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final TypeService typeService;

    public OpenFinanceImportService(
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            UserService userService,
            TypeService typeService) {
        this.connectionRepository = connectionRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.typeService = typeService;
    }

    @Transactional
    public ImportResult importSnapshot(long userId, OpenFinanceSnapshot snapshot, Instant syncedAt) {
        if (snapshot.accounts() == null || snapshot.accounts().isEmpty()) {
            throw new OpenFinanceUnavailableException("The consent did not return any accounts");
        }

        OpenFinanceConnection connection = connectionRepository
                .findByUserIdAndInstitutionId(userId, snapshot.institutionId())
                .orElseGet(OpenFinanceConnection::new);
        connection.setUser(userService.getReferenceById(userId));
        connection.setInstitutionId(snapshot.institutionId());
        connection.setInstitutionName(snapshot.institutionName());
        connection.setConsentId(snapshot.consentId());
        connection.setStatus(OpenFinanceConnectionStatus.AUTHORISED);
        connection.setAuthorisedAt(snapshot.authorisedAt());
        connection.setExpiresAt(snapshot.expiresAt());
        connection.setLastSyncedAt(syncedAt);
        connectionRepository.save(connection);

        int newTransactions = 0;
        for (OpenFinanceSnapshot.Account account : snapshot.accounts()) {
            validateBalance(account.availableBalance());
            Wallet wallet = walletRepository
                    .findByUserIdAndSourceAndExternalAccountId(userId, WalletSource.OPEN_FINANCE, account.accountId())
                    .orElseGet(Wallet::new);
            if (wallet.getId() == null) {
                wallet.setUser(userService.getReferenceById(userId));
                wallet.setIban(syntheticIban(snapshot.institutionId(), account.accountId()));
                wallet.setExternalAccountId(account.accountId());
                wallet.setSource(WalletSource.OPEN_FINANCE);
            }
            wallet.setName(accountName(account));
            wallet.setInstitutionName(account.brandName());
            wallet.setBalance(account.availableBalance());
            wallet.setLastSyncedAt(syncedAt);
            walletRepository.save(wallet);

            for (OpenFinanceSnapshot.Entry entry : account.transactions()) {
                String externalId = snapshot.institutionId() + ":" + account.accountId() + ":" + entry.transactionId();
                if (transactionRepository.existsByExternalId(externalId)) continue;
                validateEntry(entry);

                Transaction transaction = new Transaction();
                transaction.setAmount(entry.amount().setScale(2, java.math.RoundingMode.HALF_EVEN));
                transaction.setDescription(truncate(entry.transactionName(), 200));
                transaction.setCreatedAt(entry.transactionDateTime());
                transaction.setReferenceNumber(UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8)));
                transaction.setStatus(Status.SUCCESS);
                transaction.setSource(TransactionSource.OPEN_FINANCE);
                transaction.setExternalId(externalId);
                transaction.setFromWallet(wallet);
                transaction.setToWallet(wallet);
                transaction.setType(typeService.getReferenceById(
                        "CREDITO".equals(entry.creditDebitType()) ? 4L : 5L));
                transactionRepository.save(transaction);
                newTransactions++;
            }
        }
        return new ImportResult(snapshot.accounts().size(), newTransactions);
    }

    private void validateBalance(BigDecimal balance) {
        if (balance == null || balance.abs().compareTo(MAX_AMOUNT) > 0 || balance.stripTrailingZeros().scale() > 2) {
            throw new OpenFinanceUnavailableException("The account balance is outside the supported range");
        }
    }

    private void validateEntry(OpenFinanceSnapshot.Entry entry) {
        if (!("CREDITO".equals(entry.creditDebitType()) || "DEBITO".equals(entry.creditDebitType())) ||
                entry.amount() == null || entry.amount().signum() <= 0 || entry.amount().compareTo(MAX_AMOUNT) > 0 ||
                entry.transactionDateTime() == null) {
            throw new OpenFinanceUnavailableException("The Open Finance transaction is invalid");
        }
    }

    private String accountName(OpenFinanceSnapshot.Account account) {
        String number = account.number() == null ? "" : account.number();
        String suffix = number.length() <= 4 ? number : number.substring(number.length() - 4);
        return truncate(account.brandName() + (suffix.isBlank() ? "" : " • " + suffix), 50);
    }

    private String syntheticIban(String institutionId, String accountId) {
        UUID seed = UUID.nameUUIDFromBytes((institutionId + ":" + accountId).getBytes(StandardCharsets.UTF_8));
        java.math.BigInteger value = new java.math.BigInteger(seed.toString().replace("-", ""), 16)
                .mod(java.math.BigInteger.TEN.pow(25));
        String digits = String.format("%025d", value);
        int checksum = 98 - new java.math.BigInteger(digits + "112700")
                .mod(java.math.BigInteger.valueOf(97)).intValue();
        return "BR" + String.format("%02d", checksum) + digits;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) return "Movimentação Open Finance";
        String cleaned = value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    public record ImportResult(int accountsSynced, int newTransactions) {}
}
