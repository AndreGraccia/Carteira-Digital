package com.github.yildizmy.service.openfinance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yildizmy.exception.OpenFinanceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.open-finance.mode", havingValue = "participant-sandbox")
public class ParticipantSandboxOpenFinanceGateway implements OpenFinanceGateway {

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final HttpClient client;
    private final String baseUrl;
    private final String accessToken;
    private final String consentId;
    private final String configuredInstitutionId;

    public ParticipantSandboxOpenFinanceGateway(
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.open-finance.base-url}") String baseUrl,
            @Value("${app.open-finance.access-token}") String accessToken,
            @Value("${app.open-finance.consent-id}") String consentId,
            @Value("${app.open-finance.institution-id:participant-sandbox}") String institutionId,
            @Value("${app.open-finance.key-store-path:}") String keyStorePath,
            @Value("${app.open-finance.key-store-password:}") String keyStorePassword) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.accessToken = accessToken;
        this.consentId = consentId;
        this.configuredInstitutionId = institutionId;
        this.client = httpClient(keyStorePath, keyStorePassword);
        if (this.baseUrl.isBlank() || accessToken.isBlank() || consentId.isBlank()) {
            throw new IllegalStateException(
                    "participant-sandbox requires base-url, access-token and consent-id configuration");
        }
    }

    @Override
    public OpenFinanceSnapshot fetch(long userId) {
        JsonNode accountsPayload = getJson("/accounts");
        List<OpenFinanceSnapshot.Account> accounts = new ArrayList<>();
        String institutionName = "Open Finance Sandbox";
        String institutionId = configuredInstitutionId;

        for (JsonNode accountNode : accountsPayload.path("data")) {
            String accountId = requiredText(accountNode, "accountId");
            String brandName = accountNode.path("brandName").asText(institutionName);
            institutionName = brandName;
            if (accountNode.hasNonNull("companyCnpj")) institutionId = accountNode.path("companyCnpj").asText();

            JsonNode balancePayload = getJson("/accounts/" + encode(accountId) + "/balances");
            BigDecimal balance = decimal(balancePayload.path("data").path("availableAmount").path("amount"));
            List<OpenFinanceSnapshot.Entry> entries = fetchTransactions(accountId);

            accounts.add(new OpenFinanceSnapshot.Account(
                    accountId,
                    brandName,
                    accountNode.path("type").asText("CONTA_DEPOSITO_A_VISTA"),
                    accountNode.path("branchCode").asText(""),
                    accountNode.path("number").asText(""),
                    accountNode.path("checkDigit").asText(""),
                    balance,
                    entries
            ));
        }

        Instant now = clock.instant();
        return new OpenFinanceSnapshot(
                institutionId,
                institutionName,
                consentId,
                now,
                null,
                List.copyOf(accounts)
        );
    }

    private List<OpenFinanceSnapshot.Entry> fetchTransactions(String accountId) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusMonths(12);
        List<OpenFinanceSnapshot.Entry> entries = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            String query = "?fromBookingDate=" + from + "&toBookingDate=" + to + "&page-size=100&page=" + page;
            JsonNode payload = getJson("/accounts/" + encode(accountId) + "/transactions" + query);
            for (JsonNode transaction : payload.path("data")) {
                entries.add(new OpenFinanceSnapshot.Entry(
                        requiredText(transaction, "transactionId"),
                        requiredText(transaction, "creditDebitType"),
                        requiredText(transaction, "transactionName"),
                        transaction.path("type").asText("OUTROS"),
                        decimal(transaction.path("transactionAmount").path("amount")),
                        Instant.parse(requiredText(transaction, "transactionDateTime"))
                ));
            }
            totalPages = Math.max(1, payload.path("meta").path("totalPages").asInt(1));
            page++;
        } while (page <= totalPages);
        return List.copyOf(entries);
    }

    private JsonNode getJson(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                    .header("x-fapi-auth-date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock)))
                    .header("x-customer-user-agent", "wallia-academic-open-finance/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenFinanceUnavailableException(
                        "Open Finance sandbox returned HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (OpenFinanceUnavailableException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenFinanceUnavailableException("The Open Finance sandbox request was interrupted", exception);
        } catch (Exception exception) {
            throw new OpenFinanceUnavailableException("Could not read the Open Finance sandbox", exception);
        }
    }

    private HttpClient httpClient(String keyStorePath, String keyStorePassword) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5));
            if (!keyStorePath.isBlank()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                char[] password = keyStorePassword.toCharArray();
                try (InputStream input = Files.newInputStream(Path.of(keyStorePath))) {
                    keyStore.load(input, password);
                }
                KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagers.init(keyStore, password);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagers.getKeyManagers(), null, new SecureRandom());
                builder.sslContext(sslContext);
            }
            return builder.build();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not configure the Open Finance mTLS client", exception);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new OpenFinanceUnavailableException("Open Finance response is missing " + field);
        return value;
    }

    private BigDecimal decimal(JsonNode node) {
        try {
            return new BigDecimal(node.asText()).setScale(2, java.math.RoundingMode.HALF_EVEN);
        } catch (NumberFormatException exception) {
            throw new OpenFinanceUnavailableException("Open Finance response contains an invalid amount", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public String mode() {
        return "PARTICIPANT_SANDBOX";
    }

    @Override
    public boolean simulated() {
        return false;
    }
}
