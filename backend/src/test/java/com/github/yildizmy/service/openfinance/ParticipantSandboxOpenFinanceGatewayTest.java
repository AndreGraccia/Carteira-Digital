package com.github.yildizmy.service.openfinance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;

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

    private ParticipantSandboxOpenFinanceGateway gateway(long owner) {
        return new ParticipantSandboxOpenFinanceGateway(new ObjectMapper(), Clock.systemUTC(),
                "https://sandbox.invalid", "token", "consent", "institution", "", "", owner);
    }
}
