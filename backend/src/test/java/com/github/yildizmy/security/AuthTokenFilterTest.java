package com.github.yildizmy.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenFilterTest {

    @Test
    void aTokenIssuedBeforeLogoutEverywhereIsRevoked() {
        var user = new UserDetailsImpl(1L, "ana", "x", "Ana", "T", List.of());
        assertFalse(AuthTokenFilter.revoked(user, Instant.parse("2026-09-24T10:00:00Z")), "never revoked");

        user.setTokensValidAfter(Instant.parse("2026-09-24T12:00:00.700Z"));

        assertTrue(AuthTokenFilter.revoked(user, Instant.parse("2026-09-24T11:59:59Z")));
        assertFalse(AuthTokenFilter.revoked(user, Instant.parse("2026-09-24T12:00:00Z")), "same second counts as after");
    }
}
