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
import com.github.yildizmy.service.WalletService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Wallet routes with the real security chain and ownership check; only the service is fake. */
@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, AuthEntryPointJwt.class, WalletAccess.class,
        com.github.yildizmy.config.ClockConfig.class, com.github.yildizmy.config.AppConfig.class})
@TestPropertySource(properties = "app.security.allowedOrigin=http://localhost:3001")
class WalletControllerWebTest {

    private static final String TRANSFER = "{\"amount\":10,\"fromWalletIban\":\"FROM\",\"toWalletIban\":\"TO\",\"typeId\":1}";

    @Autowired MockMvc mvc;
    @MockitoBean WalletService walletService;
    @MockitoBean WalletRepository wallets;
    @MockitoBean TransactionRepository transactions;
    @MockitoBean UserDetailsServiceImpl userDetailsService;
    @MockitoBean JwtUtils jwtUtils;
    @MockitoBean MessageSourceConfig messageConfig;
    @MockitoBean org.springframework.jdbc.core.JdbcTemplate jdbc; // admin audit interceptor

    private static UserDetailsImpl person(long id) {
        return new UserDetailsImpl(id, "user" + id, "x", "A", "B", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static String wallet(long userId, String name) {
        return "{\"name\":\"" + name + "\",\"balance\":0,\"userId\":" + userId + "}";
    }

    @Test
    void withoutATokenNothingRuns() throws Exception {
        mvc.perform(get("/api/v1/wallets/users/7")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/wallets/transfer").contentType(MediaType.APPLICATION_JSON).content(TRANSFER))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(walletService);
    }

    @Test
    void anotherUsersWalletIs403() throws Exception {
        mvc.perform(get("/api/v1/wallets/5").with(user(person(7)))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/wallets/5").with(user(person(7)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/wallets/users/8").with(user(person(7)))).andExpect(status().isForbidden());
        verifyNoInteractions(walletService);
    }

    @Test
    void theOwnerReadsTheirWallet() throws Exception {
        when(wallets.existsByIdAndUserId(5L, 7L)).thenReturn(true);
        mvc.perform(get("/api/v1/wallets/5").with(user(person(7)))).andExpect(status().isOk());
        verify(walletService).findById(5L);
    }

    @Test
    void creatingAWalletForSomeoneElseIs403() throws Exception {
        mvc.perform(post("/api/v1/wallets").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(wallet(8, "Conta"))).andExpect(status().isForbidden());
        verify(walletService, never()).create(any());
    }

    @Test
    void anInvalidWalletIsRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/api/v1/wallets").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(wallet(7, ""))).andExpect(status().isUnprocessableEntity());
        verify(walletService, never()).create(any());
    }

    @Test
    void anOwnedWalletCannotBeHandedToAnotherUser() throws Exception {
        when(wallets.existsByIdAndUserId(5L, 7L)).thenReturn(true);
        mvc.perform(put("/api/v1/wallets/5").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(wallet(8, "Conta"))).andExpect(status().isForbidden());
        verify(walletService, never()).update(anyLong(), any());
    }

    @Test
    void moneyOnlyLeavesAnIbanTheCallerOwns() throws Exception {
        mvc.perform(post("/api/v1/wallets/transfer").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(TRANSFER)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/wallets/withdrawFunds").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(TRANSFER)).andExpect(status().isForbidden());
        verifyNoInteractions(walletService);

        when(wallets.existsByIbanAndUserId("FROM", 7L)).thenReturn(true);
        mvc.perform(post("/api/v1/wallets/transfer").with(user(person(7))).contentType(MediaType.APPLICATION_JSON)
                .content(TRANSFER)).andExpect(status().isCreated());
        verify(walletService).transferFunds(any());
    }

    @Test
    void listingEveryonesWalletsIsForAdminsOnly() throws Exception {
        mvc.perform(get("/api/v1/wallets").with(user(person(7)))).andExpect(status().isForbidden());
        verifyNoInteractions(walletService);
    }
}
