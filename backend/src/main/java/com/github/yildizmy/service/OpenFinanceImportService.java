package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.CardCharge;
import com.github.yildizmy.domain.entity.CreditCard;
import com.github.yildizmy.domain.entity.Loan;
import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.domain.enums.Status;
import com.github.yildizmy.domain.enums.TransactionSource;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import com.github.yildizmy.repository.CreditCardRepository;
import com.github.yildizmy.repository.LoanRepository;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final CreditCardRepository creditCardRepository;
    private final LoanRepository loanRepository;

    public OpenFinanceImportService(
            OpenFinanceConnectionRepository connectionRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            UserService userService,
            TypeService typeService,
            CreditCardRepository creditCardRepository,
            LoanRepository loanRepository) {
        this.connectionRepository = connectionRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.typeService = typeService;
        this.creditCardRepository = creditCardRepository;
        this.loanRepository = loanRepository;
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
                wallet.setIban(syntheticIban());
                wallet.setExternalAccountId(account.accountId());
                wallet.setSource(WalletSource.OPEN_FINANCE);
            }
            wallet.setName(accountName(account));
            wallet.setInstitutionName(account.brandName());
            wallet.setBalance(account.availableBalance());
            wallet.setLastSyncedAt(syncedAt);
            walletRepository.save(wallet);

            for (OpenFinanceSnapshot.Entry entry : account.transactions()) {
                String externalId = userId + ":" + snapshot.institutionId() + ":" + account.accountId() + ":" + entry.transactionId();
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
        importCards(userId, snapshot, syncedAt);
        importLoans(userId, snapshot, syncedAt);
        return new ImportResult(snapshot.accounts().size(), newTransactions);
    }

    /**
     * Cards mirror the institution's current view on every sync: a charge that disappeared
     * upstream (refund, cancelled purchase) must also leave the projection.
     */
    private void importCards(long userId, OpenFinanceSnapshot snapshot, Instant syncedAt) {
        if (snapshot.creditCards() == null) return;
        for (OpenFinanceSnapshot.CreditCardAccount source : snapshot.creditCards()) {
            validateCard(source);
            String externalId = snapshot.institutionId() + ":" + source.creditCardAccountId();
            CreditCard card = creditCardRepository.findByUserIdAndExternalId(userId, externalId).orElseGet(CreditCard::new);
            card.setUser(userService.getReferenceById(userId));
            card.setExternalId(externalId);
            card.setInstitution(truncate(snapshot.institutionName(), 100));
            card.setName(truncate(source.name(), 60));
            card.setLastDigits(source.lastDigits());
            card.setCreditLimit(source.limitAmount());
            card.setClosingDay(source.closingDay());
            card.setDueDay(source.dueDay());
            card.setLastSyncedAt(syncedAt);
            // Update in place by externalId: clearing and re-adding would insert the new rows before
            // Hibernate deletes the orphans and trip the (card_id, external_id) unique key.
            java.util.Map<String, CardCharge> existing = card.getCharges().stream()
                    .collect(java.util.stream.Collectors.toMap(CardCharge::getExternalId, charge -> charge));
            java.util.Set<String> received = new java.util.HashSet<>();
            for (OpenFinanceSnapshot.CardTransaction item : source.transactions()) {
                validateCharge(item);
                if (!received.add(item.transactionId())) continue;
                CardCharge charge = existing.get(item.transactionId());
                if (charge == null) {
                    charge = new CardCharge();
                    charge.setCard(card);
                    card.getCharges().add(charge);
                }
                charge.setExternalId(item.transactionId());
                charge.setDescription(truncate(item.transactionName(), 120));
                charge.setPurchaseDate(item.purchaseDate());
                charge.setTotalAmount(item.amount().setScale(2, RoundingMode.HALF_EVEN));
                // ponytail: instalment rounded down, same as the sandbox bill; leftover cents are ignored.
                charge.setInstallmentAmount(item.amount().divide(BigDecimal.valueOf(item.installments()), 2, RoundingMode.DOWN));
                charge.setInstallments(item.installments());
                charge.setFirstInvoice(item.firstInvoiceDue().withDayOfMonth(1));
            }
            // A charge the institution no longer reports (refund, cancellation) leaves the projection too.
            card.getCharges().removeIf(charge -> !received.contains(charge.getExternalId()));
            creditCardRepository.save(card);
        }
    }

    private void importLoans(long userId, OpenFinanceSnapshot snapshot, Instant syncedAt) {
        if (snapshot.loans() == null) return;
        for (OpenFinanceSnapshot.LoanContract source : snapshot.loans()) {
            validateLoan(source);
            String externalId = snapshot.institutionId() + ":" + source.contractId();
            Loan loan = loanRepository.findByUserIdAndExternalId(userId, externalId).orElseGet(Loan::new);
            loan.setUser(userService.getReferenceById(userId));
            loan.setExternalId(externalId);
            loan.setInstitution(truncate(snapshot.institutionName(), 100));
            loan.setProduct(truncate(source.productName(), 80));
            loan.setContractedAmount(source.contractAmount());
            loan.setOutstandingBalance(source.outstandingBalance());
            loan.setInstallmentAmount(source.installmentAmount());
            loan.setInstallmentsTotal(source.totalInstallments());
            loan.setInstallmentsPaid(source.paidInstallments());
            loan.setNextDueDate(source.nextDueDate());
            loan.setAnnualRate(source.annualRate());
            loan.setLastSyncedAt(syncedAt);
            loanRepository.save(loan);
        }
    }

    private void validateCard(OpenFinanceSnapshot.CreditCardAccount card) {
        if (card.limitAmount() == null || card.limitAmount().signum() < 0 || card.limitAmount().compareTo(MAX_AMOUNT) > 0
                || card.closingDay() < 1 || card.closingDay() > 31 || card.dueDay() < 1 || card.dueDay() > 31
                || card.transactions() == null) {
            throw new OpenFinanceUnavailableException("The credit card account is invalid");
        }
    }

    private void validateCharge(OpenFinanceSnapshot.CardTransaction item) {
        if (item.amount() == null || item.amount().signum() <= 0 || item.amount().compareTo(MAX_AMOUNT) > 0
                || item.installments() < 1 || item.installments() > 48
                || item.purchaseDate() == null || item.firstInvoiceDue() == null) {
            throw new OpenFinanceUnavailableException("The credit card transaction is invalid");
        }
    }

    private void validateLoan(OpenFinanceSnapshot.LoanContract loan) {
        if (loan.installmentAmount() == null || loan.installmentAmount().signum() <= 0
                || loan.outstandingBalance() == null || loan.outstandingBalance().signum() < 0
                || loan.contractAmount() == null || loan.contractAmount().signum() <= 0
                || loan.totalInstallments() < 1 || loan.paidInstallments() < 0
                || loan.paidInstallments() > loan.totalInstallments()) {
            throw new OpenFinanceUnavailableException("The loan contract is invalid");
        }
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

    // Random on purpose: a seed derived from userId + accountId let anyone precompute a victim's IBAN,
    // claim it with POST /wallets and make the victim's first import fail on uc_wallet_iban.
    // Re-syncs find the wallet by externalAccountId, so the IBAN never needs to be reproducible.
    private String syntheticIban() {
        UUID seed = UUID.randomUUID();
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
