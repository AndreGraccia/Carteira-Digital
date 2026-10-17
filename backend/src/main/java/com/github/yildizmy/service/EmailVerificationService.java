package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.EmailVerificationChallenge;
import com.github.yildizmy.dto.response.EmailChallengeResponse;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.EmailVerificationChallengeRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.mail.VerificationMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Confirms that the person signing up owns the e-mail address: an 8-digit code, stored only as a BCrypt hash,
 * valid for a few minutes, limited in attempts and in sends per day. Confirming it sets
 * {@code user.email_verified_at}; login, alerts by e-mail and Open Finance all rely on that one date (ADR 0008).
 */
@Service
public class EmailVerificationService {

    private static final int CODE_FLOOR = 10_000_000;
    private static final int CODE_RANGE = 90_000_000;
    static final int MAX_SENDS_PER_DAY = 10;

    private final EmailVerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationMailSender mailSender;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final long codeTtlMinutes;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public EmailVerificationService(
            EmailVerificationChallengeRepository challengeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationMailSender mailSender,
            Clock clock,
            SecureRandom secureRandom,
            @Value("${app.email-verification.code-ttl-minutes}") long codeTtlMinutes,
            @Value("${app.email-verification.resend-cooldown-seconds}") long resendCooldownSeconds,
            @Value("${app.email-verification.max-attempts}") int maxAttempts) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.codeTtlMinutes = Math.max(1, codeTtlMinutes);
        this.resendCooldownSeconds = Math.max(10, resendCooldownSeconds);
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, 10));
    }

    @Transactional
    public EmailChallengeResponse request(long userId) {
        Instant now = clock.instant();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CodedException(
                        "EMAIL_VERIFICATION_REQUIRED", HttpStatus.PRECONDITION_REQUIRED));
        if (user.getEmailVerifiedAt() != null) {
            throw new CodedException("EMAIL_ALREADY_VERIFIED", HttpStatus.CONFLICT);
        }
        EmailVerificationChallenge challenge = challengeRepository.findByUserIdForUpdate(userId).orElse(null);

        if (challenge != null
                && now.isBefore(challenge.getResendAvailableAt())) {
            throw new CodedException(
                    "EMAIL_CODE_RESEND_WAIT",
                    HttpStatus.TOO_MANY_REQUESTS,
                    secondsUntil(now, challenge.getResendAvailableAt())
            );
        }

        boolean newSendWindow = challenge == null
                || challenge.getSendWindowStartedAt() == null
                || !now.isBefore(challenge.getSendWindowStartedAt().plus(1, ChronoUnit.DAYS));
        if (!newSendWindow && challenge.getSendCount() >= MAX_SENDS_PER_DAY) {
            throw new CodedException(
                    "EMAIL_CODE_DAILY_LIMIT",
                    HttpStatus.TOO_MANY_REQUESTS,
                    secondsUntil(now, challenge.getSendWindowStartedAt().plus(1, ChronoUnit.DAYS))
            );
        }

        String code = String.valueOf(secureRandom.nextInt(CODE_RANGE) + CODE_FLOOR);
        if (challenge == null) {
            challenge = new EmailVerificationChallenge();
            challenge.setUser(user);
        }
        if (newSendWindow) {
            challenge.setSendWindowStartedAt(now);
            challenge.setSendCount(1);
        } else {
            challenge.setSendCount(challenge.getSendCount() + 1);
        }
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(now.plus(codeTtlMinutes, ChronoUnit.MINUTES));
        challenge.setResendAvailableAt(now.plus(resendCooldownSeconds, ChronoUnit.SECONDS));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(maxAttempts);
        challenge.setConsumedAt(null);
        challenge.setVerifiedAt(null);
        // Bind this challenge to the registered address that actually receives it.
        challenge.setVerifiedEmail(user.getEmail());
        challengeRepository.saveAndFlush(challenge);

        mailSender.sendCode(user.getEmail(), user.getFirstName(), code, codeTtlMinutes);
        return response(challenge, user.getEmail());
    }

    @Transactional(noRollbackFor = CodedException.class)
    public void verify(long userId, String code) {
        Instant now = clock.instant();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CodedException(
                        "EMAIL_VERIFICATION_REQUIRED", HttpStatus.PRECONDITION_REQUIRED));
        EmailVerificationChallenge challenge = challengeRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CodedException(
                        "EMAIL_VERIFICATION_REQUIRED",
                        HttpStatus.PRECONDITION_REQUIRED
                ));

        if (challenge.getConsumedAt() != null || !user.getEmail().equals(challenge.getVerifiedEmail())) {
            throw new CodedException(
                    "EMAIL_VERIFICATION_REQUIRED",
                    HttpStatus.PRECONDITION_REQUIRED
            );
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.setConsumedAt(now);
            challengeRepository.save(challenge);
            throw new CodedException(
                    "EMAIL_CODE_EXPIRED",
                    HttpStatus.GONE
            );
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challenge.setConsumedAt(now);
            challengeRepository.save(challenge);
            throw new CodedException(
                    "EMAIL_CODE_ATTEMPTS_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        boolean validFormat = code != null && code.matches("\\d{8}");
        if (!validFormat || !passwordEncoder.matches(code, challenge.getCodeHash())) {
            int attempts = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(attempts);
            if (attempts >= challenge.getMaxAttempts()) {
                challenge.setConsumedAt(now);
                challenge.setResendAvailableAt(challenge.getExpiresAt());
            }
            challengeRepository.save(challenge);
            throw new CodedException(
                    attempts >= challenge.getMaxAttempts()
                            ? "EMAIL_CODE_ATTEMPTS_EXCEEDED"
                            : "EMAIL_CODE_INVALID",
                    attempts >= challenge.getMaxAttempts()
                            ? HttpStatus.TOO_MANY_REQUESTS
                            : HttpStatus.BAD_REQUEST
            );
        }

        challenge.setConsumedAt(now);
        challenge.setVerifiedAt(now);
        challengeRepository.save(challenge);
        user.setEmailVerifiedAt(now);
        userRepository.save(user);
    }

    private EmailChallengeResponse response(EmailVerificationChallenge challenge, String email) {
        return new EmailChallengeResponse(
                maskEmail(email),
                challenge.getExpiresAt(),
                challenge.getResendAvailableAt()
        );
    }

    private long secondsUntil(Instant now, Instant target) {
        return Math.max(1, (Duration.between(now, target).toMillis() + 999) / 1000);
    }

    static String maskEmail(String email) {
        int separator = email == null ? -1 : email.indexOf('@');
        if (separator <= 0) return "***";
        return email.substring(0, 1) + "***" + email.substring(separator);
    }
}
