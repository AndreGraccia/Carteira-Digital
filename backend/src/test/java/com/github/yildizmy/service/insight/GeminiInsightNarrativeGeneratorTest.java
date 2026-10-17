package com.github.yildizmy.service.insight;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiInsightNarrativeGeneratorTest {

    private final HttpClient client = mock(HttpClient.class);
    private final List<Long> waits = new ArrayList<>();
    private final GeminiInsightNarrativeGenerator generator = new GeminiInsightNarrativeGenerator(new ObjectMapper(),
            "key", "gemini-test", "https://gemini.invalid/v1beta/interactions", false, client, waits::add);

    @Test
    void rateLimitIsRetriedHonouringRetryAfterUntilTheLastAttempt() throws Exception {
        HttpResponse<String> limited = response(429, Map.of("Retry-After", List.of("3")));
        doReturn(limited).when(client).send(any(), any());

        assertThrows(IllegalStateException.class, this::generate);

        verify(client, times(GeminiInsightNarrativeGenerator.MAX_ATTEMPTS)).send(any(), any());
        assertEquals(List.of(3000L, 3000L), waits);
    }

    @Test
    void aBadRequestIsNotRetried() throws Exception {
        HttpResponse<String> bad = response(400, Map.of());
        doReturn(bad).when(client).send(any(), any());

        assertThrows(IllegalStateException.class, this::generate);

        verify(client, times(1)).send(any(), any());
        assertEquals(List.of(), waits);
    }

    private void generate() {
        generator.generate(new FinancialSnapshot(1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                List.of()), "pt-BR", Map.of());
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }
}
