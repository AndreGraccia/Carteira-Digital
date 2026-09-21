package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceEmailChallenge;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.exception.OpenFinanceVerificationException;
import com.github.yildizmy.repository.OpenFinanceEmailChallengeRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceVerificationMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenFinanceEmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    private OpenFinanceEmailChallengeRepository challengeRepository;
    private UserRepository userRepository;
    private OpenFinanceVerificationMailSender mailSender;
    private SecureRandom secureRandom;
    private BCryptPasswordEncoder passwordEncoder;
    private OpenFinanceEmailVerificationService service;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(OpenFinanceEmailChallengeRepository.class);
        userRepository = mock(UserRepository.class);
        mailSender = mock(OpenFinanceVerificationMailSender.class);
        secureRandom = mock(SecureRandom.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new OpenFinanceEmailVerificationService(
                challengeRepository,
                userRepository,
                passwordEncoder,
                mailSender,
                Clock.fixed(NOW, ZoneOffset.UTC),
                secureRandom,
                10,
                60,
                5
        );
    }

    @Test
    void requestStoresOnlyHashAndSendsCodeToRegisteredEmail() {
        User user = user();
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(secureRandom.nextInt(90_000_000)).thenReturn(23_456_789);

        var response = service.request(7L);

        assertEquals("m***@example.com", response.maskedEmail());
        assertEquals(NOW.plus(10, ChronoUnit.MINUTES), response.expiresAt());
        var challenge = org.mockito.ArgumentCaptor.forClass(OpenFinanceEmailChallenge.class);
        verify(challengeRepository).saveAndFlush(challenge.capture());
        assertNotEquals("33456789", challenge.getValue().getCodeHash());
        assertTrue(passwordEncoder.matches("33456789", challenge.getValue().getCodeHash()));
        verify(mailSender).sendCode("maria@example.com", "Maria", "33456789", 10);
    }

    @Test
    void verifyConsumesAValidCode() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        service.verify(7L, "87654321");

        assertEquals(NOW, challenge.getConsumedAt());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyLocksChallengeAfterLastInvalidAttempt() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 4, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var exception = assertThrows(
                OpenFinanceVerificationException.class,
                () -> service.verify(7L, "11111111")
        );

        assertEquals("OPEN_FINANCE_CODE_ATTEMPTS_EXCEEDED", exception.getCode());
        assertEquals(5, challenge.getAttemptCount());
        assertNotNull(challenge.getConsumedAt());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void requestEnforcesResendCooldownWithoutSendingAnotherEmail() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setResendAvailableAt(NOW.plusSeconds(45));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var exception = assertThrows(
                OpenFinanceVerificationException.class,
                () -> service.request(7L)
        );

        assertEquals("OPEN_FINANCE_CODE_RESEND_WAIT", exception.getCode());
        assertEquals(45, exception.getRetryAfterSeconds());
        verify(mailSender, never()).sendCode(any(), any(), any(), any(Long.class));
    }

    private OpenFinanceEmailChallenge challenge(String code, int attempts, int maximumAttempts) {
        OpenFinanceEmailChallenge challenge = new OpenFinanceEmailChallenge();
        challenge.setUser(user());
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setCreatedAt(NOW.minusSeconds(30));
        challenge.setExpiresAt(NOW.plusSeconds(300));
        challenge.setResendAvailableAt(NOW.plusSeconds(30));
        challenge.setAttemptCount(attempts);
        challenge.setMaxAttempts(maximumAttempts);
        return challenge;
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Maria");
        user.setEmail("maria@example.com");
        return user;
    }
}
