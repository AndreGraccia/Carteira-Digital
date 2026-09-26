package com.github.yildizmy.service.openfinance;

import tools.jackson.databind.ObjectMapper;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticipantSandboxOpenFinanceGatewayTest {

    @Test
    void theConfiguredConsentIsNeverReadForAnotherUser() {
        var gateway = gateway(7L);

        // Refused before any request: the token belongs to user 7.
        assertThrows(OpenFinanceUnavailableException.class, () -> gateway.fetch(8L));
    }

    @Test
    void refusesToStartWithoutAnOwner() {
        assertThrows(IllegalStateException.class, () -> gateway(0L));
    }

    @Test
    void paginationFollowsLinksNextUntilItEnds() {
        var pages = new java.util.HashMap<String, String>();
        pages.put("/accounts/v2/accounts/a/transactions", page("t1", "https://sandbox.invalid/open-banking/accounts/v2/accounts/a/transactions?pagination-key=2"));
        pages.put("https://sandbox.invalid/open-banking/accounts/v2/accounts/a/transactions?pagination-key=2", page("t2", null));
        var gateway = new ParticipantSandboxOpenFinanceGateway(new ObjectMapper(), Clock.systemUTC(),
                "https://sandbox.invalid/open-banking/accounts/v2", "token", "consent", "institution", "", "", 7L) {
            @Override
            tools.jackson.databind.JsonNode getJson(String pathOrUrl) {
                try {
                    return new ObjectMapper().readTree(pages.get(pathOrUrl));
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };

        var items = gateway.pages("/accounts/v2/accounts/a/transactions");

        assertEquals(java.util.List.of("t1", "t2"), items.stream().map(item -> item.path("transactionId").asText()).toList());
    }

    @Test
    void aNextLinkToAnotherHostIsNeverFollowed() {
        var error = assertThrows(OpenFinanceUnavailableException.class,
                () -> gateway(7L).getJson("https://attacker.example/steal"));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("outside the institution"));
    }

    private static String page(String id, String next) {
        return "{\"data\":[{\"transactionId\":\"" + id + "\"}],\"links\":{\"self\":\"x\""
                + (next == null ? "" : ",\"next\":\"" + next + "\"") + "},\"meta\":{}}";
    }

    private ParticipantSandboxOpenFinanceGateway gateway(long owner) {
        return new ParticipantSandboxOpenFinanceGateway(new ObjectMapper(), Clock.systemUTC(),
                "https://sandbox.invalid", "token", "consent", "institution", "", "", owner);
    }
}
