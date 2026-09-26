package com.github.yildizmy.controller;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.config.SecurityConfig;
import com.github.yildizmy.security.AuthEntryPointJwt;
import com.github.yildizmy.security.AuthenticationRateLimiter;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.security.UserDetailsServiceImpl;
import com.github.yildizmy.service.AccountDeletionService;
import com.github.yildizmy.service.AccountExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The caller's own account with the real security chain; only the services are fake. */
@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class,
        com.github.yildizmy.config.ClockConfig.class, com.github.yildizmy.config.AppConfig.class})
@TestPropertySource(properties = "app.security.allowedOrigin=http://localhost:3001")
class AccountControllerWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean AccountDeletionService deletion;
    @MockitoBean AccountExportService export;
    @MockitoBean AuthenticationRateLimiter rateLimiter;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean MessageSourceConfig messageConfig;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbc; // admin audit interceptor

    private static UserDetailsImpl person(long id) {
        return new UserDetailsImpl(id, "user" + id, "x", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void withoutATokenNeitherExportNorDeletionRuns() throws Exception {
        mvc.perform(get("/api/v1/account/export")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/account/delete").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(export, deletion);
    }

    @Test
    void theExportIsTheCallersOwnAsAFileAndNeverSpendsLoginAttempts() throws Exception {
        when(export.export(7L)).thenReturn(Map.of("profile", Map.of("username", "user7")));

        mvc.perform(get("/api/v1/account/export").with(user(person(7))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.matchesPattern(
                        "attachment; filename=\"wallia-dados-\\d{4}-\\d{2}-\\d{2}\\.json\"")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.profile.username").value("user7"));

        verify(export).export(7L);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void deletionWithoutAPasswordIsRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/api/v1/account/delete").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"\"}")).andExpect(status().isUnprocessableEntity());
        verify(deletion, never()).delete(anyLong(), anyString());
    }

    @Test
    void deletionAlwaysActsOnTheCallerAndCountsAsALoginAttempt() throws Exception {
        mvc.perform(post("/api/v1/account/delete").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Senha-forte-123\"}")).andExpect(status().isNoContent());

        verify(rateLimiter).acquire(org.mockito.ArgumentMatchers.eq("user7"), any());
        verify(deletion).delete(7L, "Senha-forte-123");
    }
}
