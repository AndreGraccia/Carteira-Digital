package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Type;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.TransactionSource;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFinanceImportServiceTest {

    @Mock WalletRepository wallets;
    @Mock TransactionRepository transactions;
    @Mock UserService users;
    @Mock TypeService types;
    @Mock com.github.yildizmy.repository.CreditCardRepository cards;
    @Mock com.github.yildizmy.repository.LoanRepository loans;

    private OpenFinanceImportService service;

    @BeforeEach
    void setUp() {
        service = new OpenFinanceImportService(wallets, transactions, users, types, cards, loans);
    }

    @Test
    void importsAccountBalanceAndTransactionIdempotently() {
        User user = new User();
        user.setId(7L);
        Type deposit = new Type();
        deposit.setId(4L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");

        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(types.getReferenceById(4L)).thenReturn(deposit);

        var result = service.importSnapshot(7L, snapshot("CREDITO", now), now);

        assertEquals(1, result.accountsSynced());
        assertEquals(1, result.newTransactions());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(wallets).save(walletCaptor.capture());
        assertEquals(WalletSource.OPEN_FINANCE, walletCaptor.getValue().getSource());
        assertEquals(new BigDecimal("1234.56"), walletCaptor.getValue().getBalance());
        assertTrue(walletCaptor.getValue().getIban().startsWith("BR"));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(transactionCaptor.capture());
        assertEquals(TransactionSource.OPEN_FINANCE, transactionCaptor.getValue().getSource());
        assertEquals("7:institution-1:account-1:transaction-1", transactionCaptor.getValue().getExternalId());
        assertEquals(4L, transactionCaptor.getValue().getType().getId());
    }

    @Test
    void rejectsUnknownTransactionDirection() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(OpenFinanceUnavailableException.class, () -> service.importSnapshot(7L, snapshot("OTHER", now), now));
        verify(transactions, never()).save(any());
    }

    @Test
    void sharedProviderIdsAreIsolatedBetweenUsers() {
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        Type deposit = new Type();
        deposit.setId(4L);
        when(types.getReferenceById(4L)).thenReturn(deposit);
        for (long id : new long[] {7L, 8L}) {
            User user = new User();
            user.setId(id);
            when(users.getReferenceById(id)).thenReturn(user);
            service.importSnapshot(id, snapshot("CREDITO", now), now);
        }
        var walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(wallets, org.mockito.Mockito.times(2)).save(walletCaptor.capture());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                walletCaptor.getAllValues().get(0).getIban(), walletCaptor.getAllValues().get(1).getIban());
        var transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());
        assertEquals("7:institution-1:account-1:transaction-1", transactionCaptor.getAllValues().get(0).getExternalId());
        assertEquals("8:institution-1:account-1:transaction-1", transactionCaptor.getAllValues().get(1).getExternalId());
    }

    @Test
    void resyncUpdatesCardChargesInPlaceAndDropsTheOnesNoLongerReported() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        var card = new com.github.yildizmy.domain.entity.CreditCard();
        var kept = new com.github.yildizmy.domain.entity.CardCharge();
        kept.setExternalId("tv");
        kept.setCard(card);
        var refunded = new com.github.yildizmy.domain.entity.CardCharge();
        refunded.setExternalId("refunded");
        refunded.setCard(card);
        card.getCharges().addAll(List.of(kept, refunded));

        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(any(), any(), any())).thenReturn(Optional.empty());
        when(cards.findByUserIdAndExternalId(7L, "institution-1:card-1")).thenReturn(Optional.of(card));
        var base = snapshot("CREDITO", now);
        var tv = new OpenFinanceSnapshot.CardTransaction("tv", "Smart TV", java.time.LocalDate.of(2026, 7, 14),
                new BigDecimal("3299.00"), 10, java.time.LocalDate.of(2026, 8, 3));
        var withCard = new OpenFinanceSnapshot(base.institutionId(), base.institutionName(), base.consentId(),
                base.authorisedAt(), base.expiresAt(), base.accounts(),
                List.of(new OpenFinanceSnapshot.CreditCardAccount("card-1", "Aurora", "4821",
                        new BigDecimal("8000.00"), 25, 3, List.of(tv, tv))),
                List.of());

        service.importSnapshot(7L, withCard, now);

        assertEquals(1, card.getCharges().size(), "duplicate ignored, refunded charge removed");
        assertTrue(card.getCharges().get(0) == kept, "existing row updated, not re-inserted");
        assertEquals(new BigDecimal("329.90"), kept.getInstallmentAmount());
        assertEquals(java.time.LocalDate.of(2026, 8, 1), kept.getFirstInvoice());
        verify(cards).save(card);
    }

    @Test
    void loanNoLongerReportedWasPaidOffAndLeavesTheProjection() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        var active = loan("institution-1:active");
        var paidOff = loan("institution-1:paid-off");
        var otherBank = loan("institution-2:other");
        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(any(), any(), any())).thenReturn(Optional.empty());
        when(loans.findByUserIdAndExternalId(7L, "institution-1:active")).thenReturn(Optional.of(active));
        when(loans.findByUserIdOrderById(7L)).thenReturn(List.of(active, paidOff, otherBank));
        var base = snapshot("CREDITO", now);
        var contract = new OpenFinanceSnapshot.LoanContract("active", "Pessoal", new BigDecimal("1000.00"),
                new BigDecimal("500.00"), new BigDecimal("100.00"), 10, 5, java.time.LocalDate.of(2026, 10, 5),
                null, null, null);

        service.importSnapshot(7L, new OpenFinanceSnapshot(base.institutionId(), base.institutionName(),
                base.consentId(), base.authorisedAt(), base.expiresAt(), base.accounts(), List.of(), List.of(contract)), now);

        verify(loans).delete(paidOff);
        verify(loans, never()).delete(active);
        verify(loans, never()).delete(otherBank);
    }

    @Test
    void dueDayKeepsTheContractualDayWhenOnlyAShortMonthDateArrives() {
        assertEquals(31, OpenFinanceImportService.dueDay(contract(java.time.LocalDate.of(2027, 2, 28), null), 31));
        assertEquals(28, OpenFinanceImportService.dueDay(contract(java.time.LocalDate.of(2027, 2, 28), null), null));
        assertEquals(10, OpenFinanceImportService.dueDay(contract(java.time.LocalDate.of(2027, 3, 10), null), 31),
                "a date in the middle of the month is a real change of due day");
        assertEquals(15, OpenFinanceImportService.dueDay(contract(java.time.LocalDate.of(2027, 2, 28), 15), 31),
                "the institution's own due day wins");
    }

    private OpenFinanceSnapshot.LoanContract contract(java.time.LocalDate next, Integer dueDay) {
        return new OpenFinanceSnapshot.LoanContract("c", "Pessoal", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                10, 1, next, null, dueDay, null);
    }

    private com.github.yildizmy.domain.entity.Loan loan(String externalId) {
        var loan = new com.github.yildizmy.domain.entity.Loan();
        loan.setExternalId(externalId);
        return loan;
    }

    @Test
    void scheduledEntryIsPendingUntilTheBankSettlesItAndProcessingOnesWait() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        Type withdrawal = new Type();
        withdrawal.setId(5L);
        Wallet wallet = new Wallet();
        wallet.setId(30L);
        when(types.getReferenceById(5L)).thenReturn(withdrawal);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.of(wallet));
        var rent = new OpenFinanceSnapshot.Entry("rent", "DEBITO", "Imobiliária", "PIX", new BigDecimal("1500.00"),
                Instant.parse("2026-10-01T12:00:00Z"), "LANCAMENTO_FUTURO");
        var processing = new OpenFinanceSnapshot.Entry("card", "DEBITO", "Mercado", "CARTAO", new BigDecimal("80.00"),
                now, "TRANSACAO_PROCESSANDO");

        var first = service.importSnapshot(7L, accountWith(now, rent, processing), now);

        var saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(saved.capture());
        assertEquals(com.github.yildizmy.domain.enums.Status.PENDING, saved.getValue().getStatus());
        assertEquals(0, first.newTransactions(), "a scheduled entry is not a new movement yet");

        // Next day the bank settles it under the same id.
        Transaction pending = saved.getValue();
        when(transactions.findByExternalIdIn(any())).thenReturn(List.of(pending));
        var settled = new OpenFinanceSnapshot.Entry("rent", "DEBITO", "Imobiliária", "PIX", new BigDecimal("1500.00"),
                Instant.parse("2026-10-01T12:00:00Z"), "TRANSACAO_EFETIVADA");

        var second = service.importSnapshot(7L, accountWith(now, settled), now);

        assertEquals(com.github.yildizmy.domain.enums.Status.SUCCESS, pending.getStatus());
        assertEquals(1, second.newTransactions());
    }

    @Test
    void aScheduledEntryTheBankNoLongerListsIsRemoved() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        Wallet wallet = new Wallet();
        wallet.setId(30L);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.of(wallet));
        Transaction cancelled = new Transaction();
        cancelled.setExternalId("7:institution-1:account-1:old-schedule");
        when(transactions.findByToWalletIdAndStatus(30L, com.github.yildizmy.domain.enums.Status.PENDING))
                .thenReturn(List.of(cancelled));

        service.importSnapshot(7L, accountWith(now), now);

        verify(transactions).delete(cancelled);
    }

    private OpenFinanceSnapshot accountWith(Instant now, OpenFinanceSnapshot.Entry... entries) {
        var account = new OpenFinanceSnapshot.Account("account-1", "Banco Teste", "CONTA_DEPOSITO_A_VISTA", "0001",
                "12345678", "9", new BigDecimal("1234.56"), List.of(entries));
        return new OpenFinanceSnapshot("institution-1", "Banco Teste", "consent-1", now, null, List.of(account));
    }

    private OpenFinanceSnapshot snapshot(String direction, Instant now) {
        var entry = new OpenFinanceSnapshot.Entry(
                "transaction-1", direction, "Recebimento", "PIX", new BigDecimal("100.00"), now.minusSeconds(60));
        var account = new OpenFinanceSnapshot.Account(
                "account-1", "Banco Teste", "CONTA_DEPOSITO_A_VISTA", "0001", "12345678", "9",
                new BigDecimal("1234.56"), List.of(entry));
        return new OpenFinanceSnapshot(
                "institution-1", "Banco Teste", "consent-1", now.minusSeconds(120), now.plusSeconds(3600),
                List.of(account));
    }
}
