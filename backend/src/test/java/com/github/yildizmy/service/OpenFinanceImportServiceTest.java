package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Type;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.TransactionSource;
import com.github.yildizmy.domain.enums.WalletSource;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
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

    @Mock OpenFinanceConnectionRepository connections;
    @Mock WalletRepository wallets;
    @Mock TransactionRepository transactions;
    @Mock UserService users;
    @Mock TypeService types;

    private OpenFinanceImportService service;

    @BeforeEach
    void setUp() {
        service = new OpenFinanceImportService(connections, wallets, transactions, users, types);
    }

    @Test
    void importsAccountBalanceAndTransactionIdempotently() {
        User user = new User();
        user.setId(7L);
        Type deposit = new Type();
        deposit.setId(4L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");

        when(connections.findByUserIdAndInstitutionId(7L, "institution-1")).thenReturn(Optional.empty());
        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.existsByExternalId("institution-1:account-1:transaction-1")).thenReturn(false);
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
        assertEquals("institution-1:account-1:transaction-1", transactionCaptor.getValue().getExternalId());
        assertEquals(4L, transactionCaptor.getValue().getType().getId());
    }

    @Test
    void rejectsUnknownTransactionDirection() {
        User user = new User();
        user.setId(7L);
        Instant now = Instant.parse("2026-09-20T12:00:00Z");
        when(connections.findByUserIdAndInstitutionId(7L, "institution-1")).thenReturn(Optional.empty());
        when(users.getReferenceById(7L)).thenReturn(user);
        when(wallets.findByUserIdAndSourceAndExternalAccountId(7L, WalletSource.OPEN_FINANCE, "account-1"))
                .thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.existsByExternalId("institution-1:account-1:transaction-1")).thenReturn(false);

        assertThrows(OpenFinanceUnavailableException.class, () -> service.importSnapshot(7L, snapshot("OTHER", now), now));
        verify(transactions, never()).save(any());
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
