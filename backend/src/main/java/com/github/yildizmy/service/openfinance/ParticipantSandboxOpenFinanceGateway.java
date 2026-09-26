package com.github.yildizmy.service.openfinance;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

/**
 * Reads one real sandbox consent through a pre-issued access token.
 * <p>
 * The token and the consent belong to one person, so the gateway is bound to one Wall.ia user
 * ({@code app.open-finance.owner-user-id}). Any other user is refused: without that, every account would import
 * the same bank data. Serving many users needs the authorisation flow (FAPI redirect + per-user tokens), which a
 * dev sandbox does not have.
 */
@Component
@ConditionalOnProperty(name = "app.open-finance.mode", havingValue = "participant-sandbox")
public class ParticipantSandboxOpenFinanceGateway implements OpenFinanceGateway {

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final HttpClient client;
    static final int MAX_PAGES = 50;
    /** Root of the APIs, e.g. https://host/open-banking; each API adds its own /name/vN. */
    private final String apiRoot;
    private final String accessToken;
    private final String consentId;
    private final String configuredInstitutionId;
    private final long ownerUserId;

    public ParticipantSandboxOpenFinanceGateway(
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.open-finance.base-url}") String baseUrl,
            @Value("${app.open-finance.access-token}") String accessToken,
            @Value("${app.open-finance.consent-id}") String consentId,
            @Value("${app.open-finance.institution-id:participant-sandbox}") String institutionId,
            @Value("${app.open-finance.key-store-path:}") String keyStorePath,
            @Value("${app.open-finance.key-store-password:}") String keyStorePassword,
            @Value("${app.open-finance.owner-user-id:0}") long ownerUserId) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        // Before the cards and loans APIs, base-url pointed at .../accounts/v2; both forms are accepted.
        this.apiRoot = baseUrl.replaceAll("/+$", "").replaceAll("/accounts/v2$", "");
        this.accessToken = accessToken;
        this.consentId = consentId;
        this.configuredInstitutionId = institutionId;
        this.ownerUserId = ownerUserId;
        this.client = httpClient(keyStorePath, keyStorePassword);
        if (this.apiRoot.isBlank() || accessToken.isBlank() || consentId.isBlank()) {
            throw new IllegalStateException(
                    "participant-sandbox requires base-url, access-token and consent-id configuration");
        }
        if (ownerUserId <= 0) {
            throw new IllegalStateException(
                    "participant-sandbox requires owner-user-id: the configured consent belongs to one user");
        }
    }

    @Override
    public OpenFinanceSnapshot fetch(long userId) {
        if (userId != ownerUserId) {
            throw new OpenFinanceUnavailableException("This sandbox consent belongs to another user");
        }
        // The person can revoke in the bank's app: ask the institution before reading anything under the consent.
        OfbResponses.requireAuthorised(getJson("/consents/v3/consents/" + encode(consentId)).path("data"));
        LocalDate today = LocalDate.now(clock.withZone(OfbResponses.ZONE));
        JsonNode resources = optional("/resources/v3/resources");
        String institutionName = "Open Finance Sandbox";
        String institutionId = configuredInstitutionId;

        List<OpenFinanceSnapshot.Account> accounts = new ArrayList<>();
        for (JsonNode accountNode : pages("/accounts/v2/accounts")) {
            String accountId = OfbResponses.text(accountNode, "accountId");
            if (!isAvailable(resources, "ACCOUNT", accountId)) continue;
            institutionName = accountNode.path("brandName").asText(institutionName);
            if (accountNode.hasNonNull("companyCnpj")) institutionId = accountNode.path("companyCnpj").asText();
            String base = "/accounts/v2/accounts/" + encode(accountId);
            BigDecimal balance = OfbResponses.availableBalance(getJson(base + "/balances").path("data"));
            List<OpenFinanceSnapshot.Entry> entries = new ArrayList<>();
            for (JsonNode transaction : pages(base + "/transactions?fromBookingDate=" + today.minusMonths(12)
                    + "&toBookingDate=" + today.plusMonths(3) + "&page-size=1000")) {
                entries.add(OfbResponses.entry(transaction));
            }
            accounts.add(new OpenFinanceSnapshot.Account(accountId, institutionName,
                    accountNode.path("type").asText("CONTA_DEPOSITO_A_VISTA"),
                    accountNode.path("branchCode").asText(""), accountNode.path("number").asText(""),
                    accountNode.path("checkDigit").asText(""), balance, List.copyOf(entries)));
        }

        return new OpenFinanceSnapshot(institutionId, institutionName, consentId, clock.instant(), null,
                List.copyOf(accounts), cards(resources, today), loans(resources, today));
    }

    /** Null when the institution does not share cards under this consent: "not read", so nothing is deleted. */
    private List<OpenFinanceSnapshot.CreditCardAccount> cards(JsonNode resources, LocalDate today) {
        try {
            List<OpenFinanceSnapshot.CreditCardAccount> cards = new ArrayList<>();
            for (JsonNode account : pages("/credit-cards-accounts/v2/accounts")) {
                String id = OfbResponses.text(account, "creditCardAccountId");
                if (!isAvailable(resources, "CREDIT_CARD_ACCOUNT", id)) continue;
                String base = "/credit-cards-accounts/v2/accounts/" + encode(id);
                cards.add(OfbResponses.card(account, getJson(base + "/limits").path("data"),
                        getJson(base + "/bills?fromDueDate=" + today.minusMonths(12) + "&toDueDate=" + today.plusMonths(2)).path("data"),
                        pages(base + "/transactions?fromTransactionDate=" + today.minusMonths(12)
                                + "&toTransactionDate=" + today + "&page-size=1000"),
                        today));
            }
            return List.copyOf(cards);
        } catch (OpenFinanceUnavailableException exception) {
            return null;
        }
    }

    /** Null when loans are not shared under this consent. Non-monthly contracts are left out, not guessed. */
    private List<OpenFinanceSnapshot.LoanContract> loans(JsonNode resources, LocalDate today) {
        try {
            List<OpenFinanceSnapshot.LoanContract> loans = new ArrayList<>();
            for (JsonNode contract : pages("/loans/v2/contracts")) {
                String id = OfbResponses.text(contract, "contractId");
                if (!isAvailable(resources, "LOAN", id)) continue;
                String base = "/loans/v2/contracts/" + encode(id);
                OpenFinanceSnapshot.LoanContract loan = OfbResponses.loan(id, getJson(base).path("data"),
                        getJson(base + "/scheduled-instalments").path("data"), getJson(base + "/payments").path("data"), today);
                if (loan != null) loans.add(loan);
            }
            return List.copyOf(loans);
        } catch (OpenFinanceUnavailableException exception) {
            return null;
        }
    }

    /** Without /resources (not every sandbox has it) every listed resource is read. */
    private static boolean isAvailable(JsonNode resources, String type, String id) {
        if (resources == null) return true;
        return OfbResponses.available(resources.path("data"), type).contains(id);
    }

    private JsonNode optional(String path) {
        try {
            return getJson(path);
        } catch (OpenFinanceUnavailableException exception) {
            return null;
        }
    }

    /**
     * Every item of a paginated list, following links.next as the standard says (the meta of Accounts 2.4 has
     * no page count, so a page-number loop stops after the first page). Stops at MAX_PAGES pages.
     */
    List<JsonNode> pages(String path) {
        List<JsonNode> items = new ArrayList<>();
        String next = path;
        for (int page = 0; next != null && !next.isBlank(); page++) {
            if (page == MAX_PAGES) throw new OpenFinanceUnavailableException("Open Finance pagination did not end");
            JsonNode payload = getJson(next);
            payload.path("data").forEach(items::add);
            String link = payload.path("links").path("next").asText(null);
            next = link == null || link.equals(payload.path("links").path("self").asText(null)) ? null : link;
        }
        return items;
    }

    /** A path under the API root, or an absolute links.next URL, which must point at the same host. */
    JsonNode getJson(String pathOrUrl) {
        URI root = URI.create(apiRoot);
        URI uri = pathOrUrl.startsWith("http") ? URI.create(pathOrUrl) : URI.create(apiRoot + pathOrUrl);
        if (!java.util.Objects.equals(uri.getScheme(), root.getScheme())
                || !java.util.Objects.equals(uri.getHost(), root.getHost()) || uri.getPort() != root.getPort()) {
            // links.next comes from the institution; following it anywhere else would be SSRF.
            throw new OpenFinanceUnavailableException("Open Finance link points outside the institution");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public String institutionId() {
        return configuredInstitutionId;
    }

    @Override
    public String institutionName() {
        return "Open Finance Sandbox";
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
