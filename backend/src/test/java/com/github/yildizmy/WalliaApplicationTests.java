package com.github.yildizmy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yildizmy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application against a real PostgreSQL: Spring context, Flyway, every JPQL query, security rules and the
 * main journeys over HTTP. Runs when WALLIA_IT_DB_URL points to a database (always in CI; locally with Docker, see
 * the developer guide). The datasource and secrets are overridden here, so a local .env.properties is never used.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "WALLIA_IT_DB_URL", matches = ".+")
class WalliaApplicationTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("WALLIA_IT_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("WALLIA_IT_DB_USER", "postgres"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("WALLIA_IT_DB_PASSWORD", "postgres"));
        registry.add("app.security.jwtSecret", () -> Base64.getEncoder().encodeToString(new byte[48]));
        registry.add("app.security.dataEncryptionKey", () -> Base64.getEncoder().encodeToString(new byte[32]));
        registry.add("app.ai.geminiApiKey", () -> "");
        registry.add("app.open-finance.mode", () -> "contract-sandbox");
        registry.add("app.open-finance.daily-sync-cron", () -> "-");
        registry.add("app.future.alert-cron", () -> "-");
        registry.add("app.market.refresh-cron", () -> "-");
        registry.add("app.market.refresh-on-startup", () -> "false");
        registry.add("spring.mail.port", () -> "1");
        registry.add("app.push.public-key", () -> "");
        registry.add("app.push.private-key", () -> "");
    }

    @Test
    void protectedRoutesNeedAToken() throws Exception {
        mvc.perform(get("/api/v1/future/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    void marketDataReadsTheStoredBancoCentralRatesAndTheRiskWindowSaysWhereItsRateCameFrom() throws Exception {
        mvc.perform(get("/api/v1/market/overview")).andExpect(status().isUnauthorized());
        jdbc.update("INSERT INTO market_rate (modality, period_start, period_end, cnpj8, institution, monthly_rate, annual_rate)"
                + " VALUES ('OVERDRAFT', DATE '2099-01-05', DATE '2099-01-12', '00000001', 'BANCO TESTE', 2.5, 34.49)"
                + " ON CONFLICT DO NOTHING");
        Session ana = verifiedUser("10.5.0.1");

        mvc.perform(auth(ana, get("/api/v1/market/overview")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rates[0].modality").value("OVERDRAFT"))
                .andExpect(jsonPath("$.rates[0].periodStart").value("2099-01-05"))
                .andExpect(jsonPath("$.rates[0].median").value(2.5));
        mvc.perform(auth(ana, get("/api/v1/future/overview"))).andExpect(status().isOk());
    }

    @Test
    void signupVerifiesTheEmailBeforeTheFirstLogin() throws Exception {
        String name = unique();
        mvc.perform(from("10.1.0.1", post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signup(name))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.maskedEmail").value(name.charAt(0) + "***@example.test"));

        mvc.perform(from("10.1.0.1", login(name))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // The same e-mail again: same answer, no second account.
        mvc.perform(from("10.1.0.1", post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signup("x" + name.substring(1), name + "@example.test"))))
                .andExpect(status().isAccepted());
        assertTrue(users.findByUsernameIgnoreCase("x" + name.substring(1)).isEmpty());
    }

    @Test
    void openFinanceJourneyFromConsentToRevocationAndTheStatementIsPaged() throws Exception {
        Session ana = verifiedUser("10.2.0.1");
        Session bruno = verifiedUser("10.2.0.2");

        JsonNode consent = body(mvc.perform(auth(ana, post("/api/v1/open-finance/connection")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"months\":6}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_AUTHORISATION")));
        mvc.perform(auth(ana, post("/api/v1/open-finance/connection/authorise").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentId\":\"" + consent.path("consentId").asText() + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORISED"));

        JsonNode page = body(mvc.perform(auth(ana, get("/api/v1/transactions/users/" + ana.id() + "?page=0&size=5")))
                .andExpect(status().isOk()));
        assertEquals(5, page.path("content").size());
        // What the API shows in clear is stored encrypted (ADR 0014).
        String stored = jdbc.queryForObject("SELECT t.description FROM transaction t JOIN wallet w ON w.id = t.to_wallet_id "
                + "WHERE w.user_id = ? AND t.description IS NOT NULL LIMIT 1", String.class, ana.id());
        assertTrue(stored.startsWith("enc:v1:"), stored);
        assertTrue(page.path("content").get(0).path("description").asText().length() < stored.length());
        assertTrue(page.path("page").path("totalElements").asInt() > 5, "the sandbox imports months of history");

        mvc.perform(auth(ana, get("/api/v1/future/overview"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.sources.accounts").value(2));
        mvc.perform(auth(ana, get("/api/v1/future/commitments"))).andExpect(status().isOk());

        // Another user's statement is not reachable by changing the id.
        mvc.perform(auth(bruno, get("/api/v1/transactions/users/" + ana.id()))).andExpect(status().isForbidden());

        mvc.perform(auth(ana, delete("/api/v1/open-finance/connection"))).andExpect(status().isNoContent());
        mvc.perform(auth(ana, get("/api/v1/open-finance/status"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("CUSTOMER_MANUALLY_REVOKED"))
                .andExpect(jsonPath("$.accountCount").value(0));
        assertEquals(0, body(mvc.perform(auth(ana, get("/api/v1/transactions/users/" + ana.id()))))
                .path("page").path("totalElements").asInt(), "revoking deletes what was imported");
    }

    @Autowired
    private com.github.yildizmy.service.job.JobLock jobLock;

    @Test
    void aJobAlreadyRunningElsewhereIsSkipped() throws Exception {
        java.util.concurrent.CountDownLatch holding = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Thread other = new Thread(() -> jobLock.runExclusive("it-job", () -> {
            holding.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }));
        other.start();
        holding.await();

        assertTrue(jobLock.runExclusive("it-job", () -> 2).isEmpty(), "second instance skips");
        release.countDown();
        other.join();
        assertEquals(java.util.Optional.of(3), jobLock.runExclusive("it-job", () -> 3), "free again after the run");
    }

    @Test
    void logoutEverywhereEndsTheTokensAlreadyIssued() throws Exception {
        Session ana = verifiedUser("10.4.0.1");
        mvc.perform(auth(ana, get("/api/v1/future/overview"))).andExpect(status().isOk());
        Thread.sleep(1100); // iat has second precision: the logout must fall in a later second than the login

        mvc.perform(auth(ana, post("/api/v1/auth/logout"))).andExpect(status().isNoContent());

        mvc.perform(auth(ana, get("/api/v1/future/overview"))).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingTheAccountAsksThePasswordErasesTheImportedDataAndEndsTheLogin() throws Exception {
        Session ana = verifiedUser("10.5.0.1");
        JsonNode consent = body(mvc.perform(auth(ana, post("/api/v1/open-finance/connection")
                .contentType(MediaType.APPLICATION_JSON).content("{\"months\":6}"))).andExpect(status().isCreated()));
        mvc.perform(auth(ana, post("/api/v1/open-finance/connection/authorise").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consentId\":\"" + consent.path("consentId").asText() + "\"}"))).andExpect(status().isOk());
        String name = users.findById(ana.id()).orElseThrow().getUsername();

        mvc.perform(auth(ana, post("/api/v1/account/delete").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"errada-12345\"}"))).andExpect(status().isForbidden());
        mvc.perform(auth(ana, post("/api/v1/account/delete").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Senha-forte-123\"}"))).andExpect(status().isNoContent());

        assertEquals("0", String.valueOf(jdbc.queryForObject(
                "SELECT count(*) FROM wallet WHERE user_id = ?", Long.class, ana.id())));
        assertEquals("0", String.valueOf(jdbc.queryForObject(
                "SELECT count(*) FROM open_finance_connection WHERE user_id = ?", Long.class, ana.id())));
        assertEquals("excluido#" + ana.id(), users.findById(ana.id()).orElseThrow().getUsername());
        mvc.perform(auth(ana, get("/api/v1/future/overview"))).andExpect(status().isUnauthorized());
        mvc.perform(from("10.5.0.1", login(name))).andExpect(status().isUnauthorized());
    }

    @Test
    void ruleErrorsCarryTheirOwnStatus() throws Exception {
        Session ana = verifiedUser("10.3.0.1");

        mvc.perform(auth(ana, delete("/api/v1/future/goals/999999"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
    }

    private Session verifiedUser(String address) throws Exception {
        String name = unique();
        mvc.perform(from(address, post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup(name))))
                .andExpect(status().isAccepted());
        // The code travels by e-mail; here the account is marked as verified directly.
        var user = users.findByUsernameIgnoreCase(name).orElseThrow();
        user.setEmailVerifiedAt(Instant.now());
        users.save(user);
        JsonNode token = body(mvc.perform(from(address, login(name))).andExpect(status().isOk()));
        return new Session(user.getId(), token.path("token").asText());
    }

    private static MockHttpServletRequestBuilder login(String name) {
        return post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"password\":\"Senha-forte-123\"}");
    }

    private static String signup(String name) {
        return signup(name, name + "@example.test");
    }

    private static String signup(String name, String email) {
        return "{\"firstName\":\"Ana\",\"lastName\":\"Teste\",\"username\":\"" + name + "\",\"email\":\"" + email
                + "\",\"password\":\"Senha-forte-123\"}";
    }

    private static String unique() {
        return "it" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static MockHttpServletRequestBuilder from(String address, MockHttpServletRequestBuilder request) {
        return request.with(servlet -> {
            servlet.setRemoteAddr(address);
            return servlet;
        });
    }

    private static MockHttpServletRequestBuilder auth(Session session, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + session.token());
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private record Session(long id, String token) {
    }
}
