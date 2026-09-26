package com.github.yildizmy.controller;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.config.SecurityConfig;
import com.github.yildizmy.exception.AuthenticationRateLimitException;
import com.github.yildizmy.security.AuthEntryPointJwt;
import com.github.yildizmy.security.AuthenticationRateLimiter;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.security.UserDetailsServiceImpl;
import com.github.yildizmy.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Public auth routes with the real security chain; the service and the limiter are fake. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class,
        com.github.yildizmy.config.ClockConfig.class, com.github.yildizmy.config.AppConfig.class})
@TestPropertySource(properties = "app.security.allowedOrigin=http://localhost:3001")
class AuthControllerWebTest {

    private static final String LOGIN = "{\"username\":\"ana\",\"password\":\"Senha-forte-123\"}";

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;
    @MockitoBean AuthenticationRateLimiter rateLimiter;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean MessageSourceConfig messageConfig;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbc; // admin audit interceptor

    @Test
    void aSpentLimitIs429AndThePasswordIsNeverChecked() throws Exception {
        doThrow(new AuthenticationRateLimitException()).when(rateLimiter).acquire(eq("ana"), anyString());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMIT"));
        verify(authService, never()).login(any());
    }

    @Test
    void aWrongPasswordIs401AndKeepsTheAttemptSpent() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized());
        verify(rateLimiter).acquire(eq("ana"), anyString());
        verify(rateLimiter, never()).succeeded(anyString(), anyString());
    }

    @Test
    void aWeakPasswordStopsTheSignupBeforeTheService() throws Exception {
        mvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"firstName\":\"Ana\",\"lastName\":\"Lima\",\"username\":\"ana\",\"email\":\"ana@x.com\",\"password\":\"123\"}"))
                .andExpect(status().isUnprocessableEntity());
        verify(authService, never()).signup(any());
    }

    @Test
    void confirmingTheEmailCountsAsALoginAttempt() throws Exception {
        mvc.perform(post("/api/v1/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"ana\",\"code\":\"12345678\"}")).andExpect(status().isNoContent());
        verify(rateLimiter).acquire(eq("ana"), anyString());
        verify(authService).verifyEmail("ana", "12345678");
    }

    @Test
    void logoutEndsOnlyTheCallersSessions() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        verify(authService, never()).logoutEverywhere(anyLong());

        var ana = new UserDetailsImpl(7L, "ana", "x", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(post("/api/v1/auth/logout").with(user(ana))).andExpect(status().isNoContent());
        verify(authService).logoutEverywhere(7L);
    }
}
