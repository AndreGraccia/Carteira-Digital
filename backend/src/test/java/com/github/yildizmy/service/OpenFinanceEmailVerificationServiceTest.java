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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user()));
    }

    @Test
    void requestStoresOnlyHashAndSendsCodeToRegisteredEmail() {
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(secureRandom.nextInt(90_000_000)).thenReturn(23_456_789);

        var response = service.request(7L);

        assertEquals("m***@example.com", response.maskedEmail());
        assertEquals(NOW.plus(10, ChronoUnit.MINUTES), response.expiresAt());
        var challenge = org.mockito.ArgumentCaptor.forClass(OpenFinanceEmailChallenge.class);
        verify(challengeRepository).saveAndFlush(challenge.capture());
        assertNotEquals("33456789", challenge.getValue().getCodeHash());
        assertTrue(passwordEncoder.matches("33456789", challenge.getValue().getCodeHash()));
        assertNull(challenge.getValue().getVerifiedAt());
        verify(mailSender).sendCode("maria@example.com", "Maria", "33456789", 10);
    }

    @Test
    void verifyConsumesAValidCode() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        service.verify(7L, "87654321");

        assertEquals(NOW, challenge.getConsumedAt());
        assertEquals(NOW, challenge.getVerifiedAt());
        assertEquals("maria@example.com", challenge.getVerifiedEmail());
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
        assertEquals(challenge.getExpiresAt(), challenge.getResendAvailableAt());
        assertNull(challenge.getVerifiedAt());
        verify(challengeRepository).save(challenge);

        var retry = assertThrows(OpenFinanceVerificationException.class, () -> service.request(7L));
        assertEquals("OPEN_FINANCE_CODE_RESEND_WAIT", retry.getCode());
        verifyNoInteractions(mailSender);
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
        verify(mailSender, never()).sendCode(any(), any(), any(), anyLong());
    }

    @Test
    void expiredCodeCannotCreateVerification() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setExpiresAt(NOW);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var error = assertThrows(OpenFinanceVerificationException.class,
                () -> service.verify(7L, "87654321"));

        assertEquals("OPEN_FINANCE_CODE_EXPIRED", error.getCode());
        assertNull(challenge.getVerifiedAt());
        assertEquals(NOW, challenge.getConsumedAt());
    }

    @Test
    void verifiedCodeCannotBeReplayedOrBypassResendCooldown() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        service.verify(7L, "87654321");

        var replay = assertThrows(OpenFinanceVerificationException.class,
                () -> service.verify(7L, "87654321"));
        assertEquals("OPEN_FINANCE_VERIFICATION_REQUIRED", replay.getCode());
        var resend = assertThrows(OpenFinanceVerificationException.class, () -> service.request(7L));
        assertEquals("OPEN_FINANCE_CODE_RESEND_WAIT", resend.getCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void resentCodeReplacesPreviousCodeAndClearsPreviousVerification() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setResendAvailableAt(NOW.minusSeconds(1));
        challenge.setVerifiedAt(NOW.minusSeconds(120));
        challenge.setVerifiedEmail("maria@example.com");
        challenge.setConsumedAt(NOW.minusSeconds(120));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        when(secureRandom.nextInt(90_000_000)).thenReturn(23_456_789);

        service.request(7L);

        assertNull(challenge.getVerifiedAt());
        assertEquals("maria@example.com", challenge.getVerifiedEmail());
        assertNull(challenge.getConsumedAt());
        var oldCode = assertThrows(OpenFinanceVerificationException.class,
                () -> service.verify(7L, "87654321"));
        assertEquals("OPEN_FINANCE_CODE_INVALID", oldCode.getCode());
        service.verify(7L, "33456789");
        assertEquals(NOW, challenge.getVerifiedAt());
    }

    @Test
    void anotherUsersCodeCannotVerifyCurrentUser() {
        User otherUser = user();
        otherUser.setId(8L);
        when(userRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(otherUser));
        when(challengeRepository.findByUserIdForUpdate(8L)).thenReturn(Optional.empty());

        var error = assertThrows(OpenFinanceVerificationException.class,
                () -> service.verify(8L, "87654321"));

        assertEquals("OPEN_FINANCE_VERIFICATION_REQUIRED", error.getCode());
        verify(challengeRepository, never()).save(any());
        verify(challengeRepository, never()).findByUserIdForUpdate(7L);
    }

    @Test
    void codeSentBeforeRegisteredEmailChangeCannotVerifyNewEmail() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setVerifiedEmail("old@example.com");
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var error = assertThrows(OpenFinanceVerificationException.class,
                () -> service.verify(7L, "87654321"));

        assertEquals("OPEN_FINANCE_VERIFICATION_REQUIRED", error.getCode());
        assertNull(challenge.getVerifiedAt());
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void verificationRemainsValidAfterCodeExpiryButEndsAtConsentLifetime() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setConsumedAt(NOW.minus(1, ChronoUnit.DAYS));
        challenge.setVerifiedAt(NOW.minus(1, ChronoUnit.DAYS));
        challenge.setVerifiedEmail("maria@example.com");
        challenge.setExpiresAt(NOW.minus(1, ChronoUnit.HOURS));
        when(challengeRepository.findByUserId(7L)).thenReturn(Optional.of(challenge));

        assertTrue(service.isVerified(7L));
        challenge.setVerifiedAt(NOW.minus(90, ChronoUnit.DAYS));
        assertFalse(service.isVerified(7L));
    }

    @Test
    void emailChangeAndRevocationRemoveVerification() {
        OpenFinanceEmailChallenge challenge = challenge("87654321", 0, 5);
        challenge.setConsumedAt(NOW.minusSeconds(10));
        challenge.setVerifiedAt(NOW.minusSeconds(10));
        challenge.setVerifiedEmail("old@example.com");
        when(challengeRepository.findByUserId(7L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        assertFalse(service.isVerified(7L));

        challenge.setVerifiedEmail("maria@example.com");
        assertTrue(service.isVerified(7L));
        service.invalidate(7L);
        assertFalse(service.isVerified(7L));
        assertNull(challenge.getVerifiedAt());
    }

    private OpenFinanceEmailChallenge challenge(String code, int attempts, int maximumAttempts) {
        OpenFinanceEmailChallenge challenge = new OpenFinanceEmailChallenge();
        challenge.setUser(user());
        challenge.setVerifiedEmail("maria@example.com");
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
