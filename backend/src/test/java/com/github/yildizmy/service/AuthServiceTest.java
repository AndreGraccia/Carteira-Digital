package com.github.yildizmy.service;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.dto.mapper.SignupRequestMapper;
import com.github.yildizmy.dto.request.LoginRequest;
import com.github.yildizmy.dto.request.SignupRequest;
import com.github.yildizmy.dto.response.EmailChallengeResponse;
import com.github.yildizmy.exception.CodedException;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.mail.VerificationMailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SignupRequestMapper signupRequestMapper = mock(SignupRequestMapper.class);
    private final MessageSourceConfig messageConfig = mock(MessageSourceConfig.class);
    private final EmailVerificationService verification = mock(EmailVerificationService.class);
    private final VerificationMailSender mailSender = mock(VerificationMailSender.class);
    private final AuthService authService = new AuthService(messageConfig, authenticationManager, jwtUtils,
            userRepository, signupRequestMapper, verification, mailSender, Clock.fixed(NOW, ZoneOffset.UTC));

    private LoginRequest loginRequest;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password");
        var userDetails = new UserDetailsImpl(1L, "testuser", "password", "Test", "User",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void verifiedAccountLogsIn() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("test.jwt.token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "testuser", "t@example.com", NOW)));

        var response = authService.login(loginRequest);

        assertEquals("test.jwt.token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals(Collections.singletonList("ROLE_USER"), response.getRoles());
    }

    @Test
    void unverifiedAccountGetsNoTokenEvenWithTheRightPassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "testuser", "t@example.com", null)));

        var error = assertThrows(CodedException.class, () -> authService.login(loginRequest));

        assertEquals("EMAIL_NOT_VERIFIED", error.getCode());
        verify(jwtUtils, never()).generateJwtToken(any());
    }

    @Test
    void loginPreservesPasswordWhitespace() {
        loginRequest.setUsername("  testuser  ");
        loginRequest.setPassword(" password with spaces ");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "testuser", "t@example.com", NOW)));

        authService.login(loginRequest);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("testuser", captor.getValue().getPrincipal());
        assertEquals(" password with spaces ", captor.getValue().getCredentials());
    }

    @Test
    void newSignupIsCreatedUnverifiedAndGetsACode() {
        var request = signup("newuser", "new@example.com");
        var created = user(2L, "newuser", "new@example.com", null);
        when(signupRequestMapper.toUser(request)).thenReturn(created);
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(verification.request(2L)).thenReturn(new EmailChallengeResponse("n***@example.com", NOW.plusSeconds(600), NOW.plusSeconds(60)));

        var response = authService.signup(request);

        assertEquals("n***@example.com", response.maskedEmail());
        assertNull(created.getEmailVerifiedAt());
        verify(userRepository).save(created);
        verify(verification).request(2L);
    }

    @Test
    void emailAlreadyRegisteredGetsTheSameAnswerAndItsOwnerIsTold() {
        var request = signup("newuser", "Maria@Example.com");
        when(userRepository.findByEmailIgnoreCase("Maria@Example.com"))
                .thenReturn(Optional.of(user(5L, "maria", "maria@example.com", NOW)));

        var response = authService.signup(request);

        assertEquals("M***@Example.com", response.maskedEmail(), "same shape as a new sign-up");
        verify(mailSender).sendAccountExists("maria@example.com", "Maria", "maria");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(verification);
    }

    @Test
    void usernameTakenIsAPlainConflict() {
        when(userRepository.existsByUsernameIgnoreCase("existinguser")).thenReturn(true);

        var error = assertThrows(CodedException.class, () -> authService.signup(signup("existinguser", "x@example.com")));

        assertEquals("USERNAME_TAKEN", error.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signupIgnoresClientAdminRoleAndUserId() {
        var request = new SignupRequest(99L, "Test", "User", "test", "test@example.com", "password", Set.of("ROLE_ADMIN"));
        when(signupRequestMapper.toUser(request)).thenReturn(user(7L, "test", "test@example.com", null));
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(verification.request(7L)).thenReturn(new EmailChallengeResponse("t***@example.com", NOW, NOW));

        authService.signup(request);

        assertEquals(Set.of("ROLE_USER"), request.getRoles());
        assertNull(request.getId());
    }

    @Test
    void unknownLoginOnVerifyLooksLikeAWrongCode() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        var error = assertThrows(CodedException.class, () -> authService.verifyEmail("ghost", "12345678"));

        assertEquals("EMAIL_CODE_INVALID", error.getCode());
    }

    @Test
    void resendNeverSaysWhetherTheLoginExists() {
        when(userRepository.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        var unknown = authService.resendCode("ghost");

        when(userRepository.findByUsernameIgnoreCase("maria")).thenReturn(Optional.of(user(5L, "maria", "m@example.com", null)));
        when(verification.request(5L)).thenReturn(new EmailChallengeResponse("m***@example.com", NOW, NOW.plusSeconds(60)));
        var known = authService.resendCode("maria");

        assertNull(unknown.maskedEmail());
        assertNull(known.maskedEmail());
        assertEquals(unknown.resendAvailableAt(), known.resendAvailableAt());
        verify(verification).request(5L);
    }

    private SignupRequest signup(String username, String email) {
        return new SignupRequest(null, "New", "User", username, email, "password", Set.of("ROLE_USER"));
    }

    private User user(long id, String username, String email, Instant verifiedAt) {
        var user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Maria");
        user.setEmailVerifiedAt(verifiedAt);
        return user;
    }
}
