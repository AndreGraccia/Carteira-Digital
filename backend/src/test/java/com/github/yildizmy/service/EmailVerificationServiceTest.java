package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.EmailVerificationChallenge;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.EmailVerificationChallengeRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.mail.VerificationMailSender;
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

class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    private EmailVerificationChallengeRepository challengeRepository;
    private UserRepository userRepository;
    private VerificationMailSender mailSender;
    private FixedRandom secureRandom;
    private BCryptPasswordEncoder passwordEncoder;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(EmailVerificationChallengeRepository.class);
        userRepository = mock(UserRepository.class);
        mailSender = mock(VerificationMailSender.class);
        secureRandom = new FixedRandom();
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new EmailVerificationService(
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
        secureRandom.value = 23_456_789;

        var response = service.request(7L);

        assertEquals("m***@example.com", response.maskedEmail());
        assertEquals(NOW.plus(10, ChronoUnit.MINUTES), response.expiresAt());
        var challenge = org.mockito.ArgumentCaptor.forClass(EmailVerificationChallenge.class);
        verify(challengeRepository).saveAndFlush(challenge.capture());
        assertNotEquals("33456789", challenge.getValue().getCodeHash());
        assertTrue(passwordEncoder.matches("33456789", challenge.getValue().getCodeHash()));
        assertNull(challenge.getValue().getVerifiedAt());
        verify(mailSender).sendCode("maria@example.com", "Maria", "33456789", 10);
    }

    @Test
    void verifyConsumesAValidCode() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        service.verify(7L, "87654321");

        assertEquals(NOW, challenge.getConsumedAt());
        assertEquals(NOW, challenge.getVerifiedAt());
        assertEquals("maria@example.com", challenge.getVerifiedEmail());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyLocksChallengeAfterLastInvalidAttempt() {
        EmailVerificationChallenge challenge = challenge("87654321", 4, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var exception = assertThrows(
                CodedException.class,
                () -> service.verify(7L, "11111111")
        );

        assertEquals("EMAIL_CODE_ATTEMPTS_EXCEEDED", exception.getCode());
        assertEquals(5, challenge.getAttemptCount());
        assertNotNull(challenge.getConsumedAt());
        assertEquals(challenge.getExpiresAt(), challenge.getResendAvailableAt());
        assertNull(challenge.getVerifiedAt());
        verify(challengeRepository).save(challenge);

        var retry = assertThrows(CodedException.class, () -> service.request(7L));
        assertEquals("EMAIL_CODE_RESEND_WAIT", retry.getCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void requestEnforcesResendCooldownWithoutSendingAnotherEmail() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        challenge.setResendAvailableAt(NOW.plusSeconds(45));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var exception = assertThrows(
                CodedException.class,
                () -> service.request(7L)
        );

        assertEquals("EMAIL_CODE_RESEND_WAIT", exception.getCode());
        assertEquals(45, exception.getRetryAfterSeconds());
        verify(mailSender, never()).sendCode(any(), any(), any(), anyLong());
    }

    @Test
    void expiredCodeCannotCreateVerification() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        challenge.setExpiresAt(NOW);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var error = assertThrows(CodedException.class,
                () -> service.verify(7L, "87654321"));

        assertEquals("EMAIL_CODE_EXPIRED", error.getCode());
        assertNull(challenge.getVerifiedAt());
        assertEquals(NOW, challenge.getConsumedAt());
    }

    @Test
    void verifiedCodeCannotBeReplayedAndAVerifiedAccountGetsNoNewCode() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        service.verify(7L, "87654321");

        var replay = assertThrows(CodedException.class,
                () -> service.verify(7L, "87654321"));
        assertEquals("EMAIL_VERIFICATION_REQUIRED", replay.getCode());
        var resend = assertThrows(CodedException.class, () -> service.request(7L));
        assertEquals("EMAIL_ALREADY_VERIFIED", resend.getCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void resentCodeReplacesPreviousCodeAndClearsPreviousVerification() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        challenge.setResendAvailableAt(NOW.minusSeconds(1));
        challenge.setVerifiedAt(NOW.minusSeconds(120));
        challenge.setVerifiedEmail("maria@example.com");
        challenge.setConsumedAt(NOW.minusSeconds(120));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));
        secureRandom.value = 23_456_789;

        service.request(7L);

        assertNull(challenge.getVerifiedAt());
        assertEquals("maria@example.com", challenge.getVerifiedEmail());
        assertNull(challenge.getConsumedAt());
        var oldCode = assertThrows(CodedException.class,
                () -> service.verify(7L, "87654321"));
        assertEquals("EMAIL_CODE_INVALID", oldCode.getCode());
        service.verify(7L, "33456789");
        assertEquals(NOW, challenge.getVerifiedAt());
    }

    @Test
    void anotherUsersCodeCannotVerifyCurrentUser() {
        User otherUser = user();
        otherUser.setId(8L);
        when(userRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(otherUser));
        when(challengeRepository.findByUserIdForUpdate(8L)).thenReturn(Optional.empty());

        var error = assertThrows(CodedException.class,
                () -> service.verify(8L, "87654321"));

        assertEquals("EMAIL_VERIFICATION_REQUIRED", error.getCode());
        verify(challengeRepository, never()).save(any());
        verify(challengeRepository, never()).findByUserIdForUpdate(7L);
    }

    @Test
    void codeSentBeforeRegisteredEmailChangeCannotVerifyNewEmail() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        challenge.setVerifiedEmail("old@example.com");
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var error = assertThrows(CodedException.class,
                () -> service.verify(7L, "87654321"));

        assertEquals("EMAIL_VERIFICATION_REQUIRED", error.getCode());
        assertNull(challenge.getVerifiedAt());
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void aValidCodeMarksTheAccountAsVerified() {
        User user = user();
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge("87654321", 0, 5)));

        service.verify(7L, "87654321");

        assertEquals(NOW, user.getEmailVerifiedAt());
        verify(userRepository).save(user);
    }

    @Test
    void aVerifiedAccountDoesNotGetMoreCodes() {
        User user = user();
        user.setEmailVerifiedAt(NOW.minusSeconds(60));
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        var error = assertThrows(CodedException.class, () -> service.request(7L));

        assertEquals("EMAIL_ALREADY_VERIFIED", error.getCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void requestStopsAtTheDailySendLimitAndReopensAfter24Hours() {
        EmailVerificationChallenge challenge = challenge("87654321", 0, 5);
        challenge.setResendAvailableAt(NOW.minusSeconds(1));
        challenge.setSendWindowStartedAt(NOW.minus(2, ChronoUnit.HOURS));
        challenge.setSendCount(EmailVerificationService.MAX_SENDS_PER_DAY);
        when(challengeRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(challenge));

        var exception = assertThrows(CodedException.class, () -> service.request(7L));
        assertEquals("EMAIL_CODE_DAILY_LIMIT", exception.getCode());
        verifyNoInteractions(mailSender);

        challenge.setSendWindowStartedAt(NOW.minus(25, ChronoUnit.HOURS));
        service.request(7L);
        assertEquals(1, challenge.getSendCount());
        assertEquals(NOW, challenge.getSendWindowStartedAt());
    }

    private EmailVerificationChallenge challenge(String code, int attempts, int maximumAttempts) {
        EmailVerificationChallenge challenge = new EmailVerificationChallenge();
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

    // Mockito cannot mock SecureRandom on Java 21+ (sealed internal annotations); a fixed subclass is enough.
    private static final class FixedRandom extends SecureRandom {
        private int value;

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}
