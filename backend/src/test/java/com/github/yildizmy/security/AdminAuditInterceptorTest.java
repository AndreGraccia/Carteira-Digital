package com.github.yildizmy.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminAuditInterceptorTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AdminAuditInterceptor interceptor =
            new AdminAuditInterceptor(jdbc, Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC));

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCallsAreWrittenWithMethodPathAndStatusButNoQueryString() throws Exception {
        signIn(1L, "ROLE_ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets");
        request.setQueryString("page=2&email=someone@example.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handler("listAll"), null);

        verify(jdbc).update(anyString(), eq(1L), eq("GET"), eq("/api/v1/wallets"), eq(200),
                eq(OffsetDateTime.parse("2026-09-23T12:00:00Z")));
    }

    @Test
    void deniedAttemptsByRegularUsersAreKeptToo() throws Exception {
        signIn(7L, "ROLE_USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        interceptor.afterCompletion(new MockHttpServletRequest("GET", "/api/v1/transactions"), response, handler("listAll"), null);

        verify(jdbc).update(anyString(), eq(7L), eq("GET"), eq("/api/v1/transactions"), eq(403),
                eq(OffsetDateTime.parse("2026-09-23T12:00:00Z")));
    }

    @Test
    void ordinaryEndpointsAreNotAudited() throws Exception {
        signIn(7L, "ROLE_USER");

        interceptor.afterCompletion(new MockHttpServletRequest("GET", "/api/v1/future/overview"),
                new MockHttpServletResponse(), handler("ownData"), null);

        verifyNoInteractions(jdbc);
    }

    @Test
    void anonymousCallsHaveNoActorAndAreNotWritten() throws Exception {
        interceptor.afterCompletion(new MockHttpServletRequest("GET", "/api/v1/wallets"),
                new MockHttpServletResponse(), handler("listAll"), null);

        verify(jdbc, never()).update(anyString(), (Object[]) org.mockito.ArgumentMatchers.any());
    }

    private static void signIn(long id, String role) {
        UserDetailsImpl user = new UserDetailsImpl(id, "user" + id, "hash", "Ana", "Teste",
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static HandlerMethod handler(String name) throws NoSuchMethodException {
        return new HandlerMethod(new Endpoints(), Endpoints.class.getMethod(name));
    }

    static class Endpoints {
        @PreAuthorize("hasRole('ADMIN')")
        public void listAll() {
        }

        @PreAuthorize("@walletAccess.user(#userId)")
        public void ownData() {
        }
    }
}
