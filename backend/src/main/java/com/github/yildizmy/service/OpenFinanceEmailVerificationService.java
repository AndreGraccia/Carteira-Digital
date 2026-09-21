package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.OpenFinanceEmailChallenge;
import com.github.yildizmy.dto.response.OpenFinanceChallengeResponse;
import com.github.yildizmy.dto.response.OpenFinanceVerificationStatus;
import com.github.yildizmy.exception.OpenFinanceVerificationException;
import com.github.yildizmy.repository.OpenFinanceEmailChallengeRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.openfinance.OpenFinanceVerificationMailSender;
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

@Service
public class OpenFinanceEmailVerificationService {

    private static final int CODE_FLOOR = 10_000_000;
    private static final int CODE_RANGE = 90_000_000;

    private final OpenFinanceEmailChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OpenFinanceVerificationMailSender mailSender;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final long codeTtlMinutes;
    private final long resendCooldownSeconds;
    private final int maxAttempts;

    public OpenFinanceEmailVerificationService(
            OpenFinanceEmailChallengeRepository challengeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OpenFinanceVerificationMailSender mailSender,
            Clock clock,
            SecureRandom secureRandom,
            @Value("${app.open-finance.verification.code-ttl-minutes}") long codeTtlMinutes,
            @Value("${app.open-finance.verification.resend-cooldown-seconds}") long resendCooldownSeconds,
            @Value("${app.open-finance.verification.max-attempts}") int maxAttempts) {
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
    public OpenFinanceChallengeResponse request(long userId) {
        Instant now = clock.instant();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new OpenFinanceVerificationException(
                        "OPEN_FINANCE_VERIFICATION_REQUIRED", HttpStatus.PRECONDITION_REQUIRED));
        OpenFinanceEmailChallenge challenge = challengeRepository.findByUserIdForUpdate(userId).orElse(null);

        if (challenge != null
                && now.isBefore(challenge.getResendAvailableAt())) {
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_CODE_RESEND_WAIT",
                    HttpStatus.TOO_MANY_REQUESTS,
                    secondsUntil(now, challenge.getResendAvailableAt())
            );
        }

        String code = String.valueOf(secureRandom.nextInt(CODE_RANGE) + CODE_FLOOR);
        if (challenge == null) {
            challenge = new OpenFinanceEmailChallenge();
            challenge.setUser(user);
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

    @Transactional(noRollbackFor = OpenFinanceVerificationException.class)
    public void verify(long userId, String code) {
        Instant now = clock.instant();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new OpenFinanceVerificationException(
                        "OPEN_FINANCE_VERIFICATION_REQUIRED", HttpStatus.PRECONDITION_REQUIRED));
        OpenFinanceEmailChallenge challenge = challengeRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new OpenFinanceVerificationException(
                        "OPEN_FINANCE_VERIFICATION_REQUIRED",
                        HttpStatus.PRECONDITION_REQUIRED
                ));

        if (challenge.getConsumedAt() != null || !user.getEmail().equals(challenge.getVerifiedEmail())) {
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_VERIFICATION_REQUIRED",
                    HttpStatus.PRECONDITION_REQUIRED
            );
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.setConsumedAt(now);
            challengeRepository.save(challenge);
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_CODE_EXPIRED",
                    HttpStatus.GONE
            );
        }
        if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
            challenge.setConsumedAt(now);
            challengeRepository.save(challenge);
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_CODE_ATTEMPTS_EXCEEDED",
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
            throw new OpenFinanceVerificationException(
                    attempts >= challenge.getMaxAttempts()
                            ? "OPEN_FINANCE_CODE_ATTEMPTS_EXCEEDED"
                            : "OPEN_FINANCE_CODE_INVALID",
                    attempts >= challenge.getMaxAttempts()
                            ? HttpStatus.TOO_MANY_REQUESTS
                            : HttpStatus.BAD_REQUEST
            );
        }

        challenge.setConsumedAt(now);
        challenge.setVerifiedAt(now);
        challengeRepository.save(challenge);
    }

    @Transactional(readOnly = true)
    public boolean isVerified(long userId) {
        return challengeRepository.findByUserId(userId)
                .filter(challenge -> challenge.getVerifiedAt() != null)
                .filter(challenge -> challenge.getVerifiedAt().plus(90, ChronoUnit.DAYS).isAfter(clock.instant()))
                .filter(challenge -> challenge.getUser().getEmail().equals(challenge.getVerifiedEmail()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public OpenFinanceVerificationStatus status(long userId) {
        Instant now = clock.instant();
        return challengeRepository.findByUserId(userId)
                .filter(challenge -> challenge.getConsumedAt() == null)
                .filter(challenge -> now.isBefore(challenge.getExpiresAt()))
                .filter(challenge -> challenge.getAttemptCount() < challenge.getMaxAttempts())
                .map(challenge -> new OpenFinanceVerificationStatus(
                        true,
                        maskEmail(challenge.getUser().getEmail()),
                        challenge.getExpiresAt(),
                        challenge.getResendAvailableAt()
                ))
                .orElseGet(OpenFinanceVerificationStatus::none);
    }

    @Transactional
    public void invalidate(long userId) {
        userRepository.findByIdForUpdate(userId);
        challengeRepository.findByUserIdForUpdate(userId).ifPresent(challenge -> {
            challenge.setVerifiedAt(null);
            challenge.setConsumedAt(clock.instant());
            challengeRepository.save(challenge);
        });
    }

    private OpenFinanceChallengeResponse response(OpenFinanceEmailChallenge challenge, String email) {
        return new OpenFinanceChallengeResponse(
                maskEmail(email),
                challenge.getExpiresAt(),
                challenge.getResendAvailableAt()
        );
    }

    private long secondsUntil(Instant now, Instant target) {
        return Math.max(1, (Duration.between(now, target).toMillis() + 999) / 1000);
    }

    private String maskEmail(String email) {
        int separator = email == null ? -1 : email.indexOf('@');
        if (separator <= 0) return "***";
        return email.substring(0, 1) + "***" + email.substring(separator);
    }
}
