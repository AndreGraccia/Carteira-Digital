package com.github.yildizmy.controller;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.config.SecurityConfig;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.security.AuthEntryPointJwt;
import com.github.yildizmy.security.JwtUtils;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.security.UserDetailsServiceImpl;
import com.github.yildizmy.security.WalletAccess;
import com.github.yildizmy.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The statement route with the real security chain and ownership check; only the service is fake. */
@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class, WalletAccess.class,
        com.github.yildizmy.config.ClockConfig.class, com.github.yildizmy.config.AppConfig.class})
@TestPropertySource(properties = "app.security.allowedOrigin=http://localhost:3001")
class TransactionControllerWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean TransactionService transactionService;
    @MockitoBean WalletRepository wallets;
    @MockitoBean TransactionRepository transactions;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean MessageSourceConfig messageConfig;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbc; // admin audit interceptor

    private static UserDetailsImpl person(long id) {
        return new UserDetailsImpl(id, "user" + id, "x", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void withoutATokenTheStatementIs401() throws Exception {
        mvc.perform(get("/api/v1/transactions/users/7")).andExpect(status().isUnauthorized());
        verify(transactionService, never()).findAllByUserId(anyLong(), any(), any());
    }

    @Test
    void anotherUsersStatementIs403AndTheServiceIsNeverCalled() throws Exception {
        mvc.perform(get("/api/v1/transactions/users/7").with(user(person(8)))).andExpect(status().isForbidden());
        verify(transactionService, never()).findAllByUserId(anyLong(), any(), any());
    }

    @Test
    void theOwnerGetsTheStatementAndThePageSizeIsCapped() throws Exception {
        when(transactionService.findAllByUserId(eq(7L), any(Instant.class), any(Pageable.class))).thenReturn(Page.empty());

        mvc.perform(get("/api/v1/transactions/users/7?size=100000").with(user(person(7)))).andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(transactionService).findAllByUserId(eq(7L), eq(Instant.EPOCH), captor.capture());
        assertEquals(PageRequest.of(0, TransactionController.MAX_PAGE_SIZE).getPageSize(), captor.getValue().getPageSize());
    }

    @Test
    void listingEveryonesTransactionsIsForAdminsOnly() throws Exception {
        mvc.perform(get("/api/v1/transactions").with(user(person(7)))).andExpect(status().isForbidden());
    }
}
