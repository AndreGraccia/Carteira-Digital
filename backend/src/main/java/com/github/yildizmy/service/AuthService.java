package com.github.yildizmy.service;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.dto.mapper.SignupRequestMapper;
import com.github.yildizmy.dto.request.LoginRequest;
import com.github.yildizmy.dto.request.SignupRequest;
import com.github.yildizmy.dto.response.SignupResponse;
import com.github.yildizmy.exception.CodedException;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import java.util.Optional;
import com.github.yildizmy.dto.response.JwtResponse;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.github.yildizmy.common.MessageKeys.*;

/**
 * Service used for Authentication related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MessageSourceConfig messageConfig;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final SignupRequestMapper signupRequestMapper;
    private final EmailVerificationService emailVerificationService;
    private final com.github.yildizmy.service.mail.VerificationMailSender mailSender;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Value("${app.email-verification.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    /**
     * Authenticates users by their credentials.
     *
     * @param request
     * @return JwtResponse
     */
    public JwtResponse login(LoginRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        final UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        // Checked after the password: an unverified account is only revealed to whoever knows its password.
        boolean verified = userRepository.findById(userDetails.getId())
                .map(user -> user.getEmailVerifiedAt() != null)
                .orElse(false);
        if (!verified) {
            SecurityContextHolder.clearContext();
            throw new CodedException("EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN);
        }
        final List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        String jwt = jwtUtils.generateJwtToken(authentication);
        log.info(messageConfig.getMessage(INFO_USER_LOGIN, request.getUsername()));
        return JwtResponse
                .builder()
                .token(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .roles(roles).build();
    }

    /**
     * Registers a user and e-mails the verification code. A username already in use is a plain conflict (it is
     * a public handle); an e-mail already in use gets the same answer as a new one and its owner is told by
     * e-mail, so the form cannot be used to find out who has an account.
     */
    public SignupResponse signup(SignupRequest request) {
        request.setId(null);
        request.setRoles(java.util.Set.of("ROLE_USER"));
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername().trim()))
            throw new CodedException("USERNAME_TAKEN", HttpStatus.CONFLICT);
        String email = request.getEmail().trim();
        SignupResponse accepted = new SignupResponse(EmailVerificationService.maskEmail(email),
                clock.instant().plusSeconds(resendCooldownSeconds));

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User owner = existing.get();
            try {
                mailSender.sendAccountExists(owner.getEmail(), owner.getFirstName(), owner.getUsername());
            } catch (CodedException exception) {
                log.warn("Account-exists notice not sent");
            }
            return accepted;
        }

        final User user = signupRequestMapper.toUser(request);
        userRepository.save(user);
        log.info(messageConfig.getMessage(INFO_USER_CREATED, user.getUsername()));
        try {
            return new SignupResponse(accepted.maskedEmail(),
                    emailVerificationService.request(user.getId()).resendAvailableAt());
        } catch (CodedException exception) {
            // The account exists; the person asks for the code again from the verification screen.
            log.warn("Sign-up code not sent: {}", exception.getCode());
            return accepted;
        }
    }

    /** Confirms the address. An unknown login gets the same answer as a wrong code. */
    public void verifyEmail(String login, String code) {
        User user = findByLogin(login)
                .orElseThrow(() -> new CodedException("EMAIL_CODE_INVALID", HttpStatus.BAD_REQUEST));
        if (user.getEmailVerifiedAt() != null) return;
        emailVerificationService.verify(user.getId(), code);
    }

    /**
     * Sends a new code. Always the same answer, whether the login exists, is already verified or has to wait,
     * so this endpoint does not tell anyone which accounts exist.
     */
    public SignupResponse resendCode(String login) {
        Optional<User> user = findByLogin(login).filter(found -> found.getEmailVerifiedAt() == null);
        // No masked address here: showing it only for accounts that exist would answer "does this login exist?".
        SignupResponse answer = new SignupResponse(null, clock.instant().plusSeconds(resendCooldownSeconds));
        if (user.isEmpty()) return answer;
        try {
            emailVerificationService.request(user.get().getId());
        } catch (CodedException exception) {
            // Waiting period or daily limit: the same answer as everyone else, the code simply does not go out.
            log.info("Verification code not resent: {}", exception.getCode());
        }
        return answer;
    }

    private Optional<User> findByLogin(String login) {
        String value = login == null ? "" : login.trim();
        return value.contains("@")
                ? userRepository.findByEmailIgnoreCase(value).or(() -> userRepository.findByUsernameIgnoreCase(value))
                : userRepository.findByUsernameIgnoreCase(value).or(() -> userRepository.findByEmailIgnoreCase(value));
    }
}
