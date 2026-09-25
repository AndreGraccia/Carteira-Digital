package com.github.yildizmy.controller;

import com.github.yildizmy.dto.request.LoginRequest;
import com.github.yildizmy.dto.request.SignupRequest;
import com.github.yildizmy.dto.response.SignupResponse;
import com.github.yildizmy.dto.request.EmailResendRequest;
import com.github.yildizmy.dto.request.EmailVerificationRequest;
import com.github.yildizmy.dto.response.JwtResponse;
import com.github.yildizmy.service.AuthService;
import com.github.yildizmy.security.AuthenticationRateLimiter;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationRateLimiter authenticationRateLimiter;

    /**
     * Authenticates users by their credentials.
     *
     * @param request
     * @return JwtResponse
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String remoteAddress = servletRequest.getRemoteAddr();
        authenticationRateLimiter.acquire(request.getUsername(), remoteAddress);
        final JwtResponse response = authService.login(request);
        authenticationRateLimiter.succeeded(request.getUsername(), remoteAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers users using their credentials and user info.
     *
     * @param request
     * @return id of the registered user
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest servletRequest) {
        authenticationRateLimiter.acquireSignup(servletRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(authService.signup(request));
    }

    /** Confirms the e-mail with the code sent at sign-up. Counts as a login attempt for rate limiting. */
    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerificationRequest request,
                                            HttpServletRequest servletRequest) {
        authenticationRateLimiter.acquire(request.login(), servletRequest.getRemoteAddr());
        authService.verifyEmail(request.login(), request.code());
        authenticationRateLimiter.succeeded(request.login(), servletRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    /** Ends every session of the user: tokens issued until now stop working. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.github.yildizmy.security.UserDetailsImpl user) {
        if (user != null) authService.logoutEverywhere(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/resend")
    public ResponseEntity<SignupResponse> resendCode(@Valid @RequestBody EmailResendRequest request,
                                                     HttpServletRequest servletRequest) {
        authenticationRateLimiter.acquireSignup(servletRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(authService.resendCode(request.login()));
    }
}
