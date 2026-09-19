from pathlib import Path
p=Path('backend/src/test/java/com/github/yildizmy/security/WalletAccessTest.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text('''package com.github.yildizmy.security;

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
''',encoding='utf-8')
p=Path('backend/src/test/java/com/github/yildizmy/service/TransactionServiceTest.java');s=p.read_text();pos=s.rfind('\n}')
s=s[:pos]+'''
    @Test
    void repeatedRequestReturnsOriginalResultAndRejectsChangedAmount() {
        var request = new TransactionRequest();
        var key = java.util.UUID.randomUUID();
        request.setRequestId(key); request.setFromWalletIban("FROM"); request.setToWalletIban("TO");
        request.setTypeId(1L); request.setAmount(new BigDecimal("10.00"));
        var transaction = new Transaction(); transaction.setId(42L); transaction.setAmount(new BigDecimal("10.00"));
        var from = new Wallet(); from.setIban("FROM");
        var to = new Wallet(); to.setIban("TO");
        var type = new com.github.yildizmy.domain.entity.Type(); type.setId(1L);
        transaction.setFromWallet(from); transaction.setToWallet(to); transaction.setType(type);
        when(transactionRepository.findByRequestId(key)).thenReturn(java.util.Optional.of(transaction));
        assertEquals(42L, transactionService.replay(request).id());
        request.setAmount(new BigDecimal("20.00"));
        assertThrows(com.github.yildizmy.exception.WalletRuleException.class, () -> transactionService.replay(request));
        verify(transactionRepository, never()).save(any());
    }
'''+s[pos:];p.write_text(s,encoding='utf-8')
p=Path('backend/src/test/java/com/github/yildizmy/service/AuthServiceTest.java');s=p.read_text();pos=s.rfind('\n}');s=s[:pos]+'''
    @Test
    void signupIgnoresClientAdminRoleAndUserId() {
        var request = new SignupRequest(99L, "Test", "User", "test", "test@example.com", "password", Set.of("ROLE_ADMIN"));
        var user = new com.github.yildizmy.domain.entity.User(); user.setId(7L);
        when(signupRequestMapper.toUser(request)).thenReturn(user);
        authService.signup(request);
        assertEquals(Set.of("ROLE_USER"), request.getRoles());
        assertNull(request.getId());
    }
'''+s[pos:];p.write_text(s,encoding='utf-8')
# Make amount labels fit compact phones.
for f in ['frontend/src/pages/transfer/BasicTabs.js','frontend/src/components/transaction-detail/index.js']:
    p=Path(f);s=p.read_text(encoding='utf-8');s=s.replace('sx={{ mt: 1, mb: 3 }}', "sx={{ mt: 1, mb: 3, overflowWrap: 'anywhere' }}").replace("sx={{ py: 2, fontVariantNumeric: 'tabular-nums' }}", "sx={{ py: 2, fontVariantNumeric: 'tabular-nums', overflowWrap: 'anywhere' }}");p.write_text(s,encoding='utf-8')
