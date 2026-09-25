package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T15:00:00Z");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository users = mock(UserRepository.class);
    private final OpenFinanceImportService openFinance = mock(OpenFinanceImportService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AccountDeletionService service =
            new AccountDeletionService(users, encoder, openFinance, jdbc, Clock.fixed(NOW, ZoneOffset.UTC));

    private User ana() {
        User user = new User();
        user.setId(7L);
        user.setUsername("ana2026");
        user.setEmail("ana@example.test");
        user.setFirstName("Ana");
        user.setLastName("Souza");
        user.setPassword(encoder.encode("Teste@12345"));
        user.setEmailVerifiedAt(NOW.minusSeconds(3600));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void aWrongPasswordDeletesNothing() {
        ana();
        CodedException error = assertThrows(CodedException.class, () -> service.delete(7L, "errada12345"));
        assertEquals("INVALID_PASSWORD", error.getCode());
        verifyNoInteractions(openFinance, jdbc);
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void theRightPasswordErasesWhatIsOnlyTheirsAndAnonymisesTheRest() {
        User user = ana();
        String oldHash = user.getPassword();

        service.delete(7L, "Teste@12345");

        verify(openFinance).purge(7L);
        AccountDeletionService.OWN_ROWS.forEach(sql -> verify(jdbc).update(sql, 7L));
        verify(jdbc).update(eq("UPDATE wallet SET name = ? WHERE user_id = ?"), eq(AccountDeletionService.CLOSED_WALLET_NAME), eq(7L));
        assertEquals("excluido-7", user.getUsername());
        assertEquals("excluido-7@invalid", user.getEmail());
        assertFalse(user.getFirstName().contains("Ana") || user.getLastName().contains("Souza"));
        assertNull(user.getEmailVerifiedAt(), "login is refused from now on");
        assertEquals(NOW, user.getTokensValidAfter(), "every token already issued stops working");
        assertFalse(encoder.matches("Teste@12345", user.getPassword()));
        assertTrue(!oldHash.equals(user.getPassword()));
        verify(users).save(user);
    }

    @Test
    void walletsWithHistoryAreNeverDeletedBecauseTheyAreAlsoTheCounterpartysStatement() {
        assertTrue(AccountDeletionService.OWN_ROWS.stream()
                .filter(sql -> sql.startsWith("DELETE FROM wallet"))
                .allMatch(sql -> sql.contains("NOT EXISTS") && sql.contains("from_wallet_id") && sql.contains("to_wallet_id")));
        assertFalse(AccountDeletionService.OWN_ROWS.stream().anyMatch(sql -> sql.startsWith("DELETE FROM transaction")));
        assertFalse(AccountDeletionService.OWN_ROWS.stream().anyMatch(sql -> sql.contains("\"user\"")));
    }
}
