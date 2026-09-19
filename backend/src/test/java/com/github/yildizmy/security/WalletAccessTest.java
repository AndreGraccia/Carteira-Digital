package com.github.yildizmy.security;

import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.entity.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletAccessTest {
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final WalletAccess access = new WalletAccess(wallets, transactions);
    private void login(long id) {
        var user = new UserDetailsImpl(id, "user", "password", "Test", "User", List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void anonymousCannotReadWallets() { assertFalse(access.user(1L)); assertFalse(access.wallet(1L)); assertFalse(access.iban("CODE")); verifyNoInteractions(wallets); }
    @Test void userCannotSelectAnotherUser() { login(1L); assertTrue(access.user(1L)); assertFalse(access.user(2L)); }
    @Test void ownershipIsCheckedForWalletIdsAndCodes() {
        login(1L);
        when(wallets.existsByIdAndUserId(10L,1L)).thenReturn(true);
        when(wallets.existsByIbanAndUserId("OWN",1L)).thenReturn(true);
        assertTrue(access.wallet(10L)); assertFalse(access.wallet(20L));
        assertTrue(access.iban("OWN")); assertFalse(access.iban("OTHER"));
    }
    @Test void onlyParticipantsCanReadTransaction() {
        var fromUser = new User(); fromUser.setId(1L);
        var toUser = new User(); toUser.setId(2L);
        var from = new Wallet(); from.setUser(fromUser);
        var to = new Wallet(); to.setUser(toUser);
        var transaction = new Transaction(); transaction.setFromWallet(from); transaction.setToWallet(to);
        when(transactions.findById(5L)).thenReturn(Optional.of(transaction));
        login(1L); assertTrue(access.transaction(5L));
        login(2L); assertTrue(access.transaction(5L));
        login(3L); assertFalse(access.transaction(5L));
        assertFalse(access.transaction(6L));
    }
}
