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
// Metrics exporters are off in tests by default; on here so /actuator/prometheus is checked like in production.
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability(tracing = false)
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
        registry.add("app.retention.cron", () -> "-");
        registry.add("app.security.rotation-cron", () -> "-");
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
    void healthProbesAnswerWithoutATokenAndOnlyHealthIsExposed() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusShowsTheJobMetricsForAlerts() throws Exception {
        jobLock.runExclusive("it-metrics", () -> 1);

        String metrics = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(metrics.contains("wallia_job_seconds_count{name=\"it-metrics\",outcome=\"success\"}"), "job timer");
        assertTrue(metrics.contains("wallia_job_last_success{name=\"it-metrics\"}"), "last success gauge");
        assertTrue(metrics.contains("hikaricp_connections_active"), "connection pool");
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
        assertTrue(stored.startsWith("enc:v2:"), stored);
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
    void exportGivesThePersonAllTheirDataReadableAndNoSecrets() throws Exception {
        mvc.perform(get("/api/v1/account/export")).andExpect(status().isUnauthorized());
        Session ana = verifiedUser("10.6.0.1");
        JsonNode consent = body(mvc.perform(auth(ana, post("/api/v1/open-finance/connection")
                .contentType(MediaType.APPLICATION_JSON).content("{\"months\":6}"))).andExpect(status().isCreated()));
        mvc.perform(auth(ana, post("/api/v1/open-finance/connection/authorise").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consentId\":\"" + consent.path("consentId").asText() + "\"}"))).andExpect(status().isOk());
        mvc.perform(auth(ana, post("/api/v1/future/goals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Viagem\",\"monthlyAmount\":200,\"dayOfMonth\":10}"))).andExpect(status().isCreated());

        var result = mvc.perform(auth(ana, get("/api/v1/account/export")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"wallia-dados-")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        String text = result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode export = json.readTree(text);

        assertEquals(users.findById(ana.id()).orElseThrow().getUsername(), export.path("profile").path("username").asText());
        assertTrue(export.path("wallets").size() >= 2, "the imported accounts");
        assertTrue(export.path("transactions").size() > 5, "the imported history");
        assertTrue(export.path("creditCards").size() >= 1 && export.path("cardCharges").size() >= 1);
        assertEquals(1, export.path("openFinanceConsents").size());
        assertEquals("Viagem", export.path("goals").get(0).path("name").asText());
        assertTrue(export.path("transactions").get(0).path("created_at").asText().matches("\\d{4}-\\d{2}-\\d{2}T.*"),
                "dates as ISO text, not epoch numbers");
        // Descriptions are stored encrypted but the person gets them in clear; secrets never leave.
        assertTrue(!text.contains("enc:"), "descriptions decrypted");
        for (String secret : java.util.List.of("\"password\"", "code_hash", "p256dh", "\"auth\"", "tokens_valid_after")) {
            assertTrue(!text.contains(secret), secret);
        }
    }

    @Autowired
    private com.github.yildizmy.service.EncryptionRotationService rotation;

    @Test
    void rotationRewritesClearTextAndOlderFormatsUnderTheCurrentKey() throws Exception {
        Session ana = verifiedUser("10.8.0.1");
        JsonNode consent = body(mvc.perform(auth(ana, post("/api/v1/open-finance/connection")
                .contentType(MediaType.APPLICATION_JSON).content("{\"months\":6}"))).andExpect(status().isCreated()));
        mvc.perform(auth(ana, post("/api/v1/open-finance/connection/authorise").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consentId\":\"" + consent.path("consentId").asText() + "\"}"))).andExpect(status().isOk());
        java.util.List<Long> ids = jdbc.queryForList("SELECT t.id FROM transaction t JOIN wallet w ON w.id = t.to_wallet_id "
                + "WHERE w.user_id = ? ORDER BY t.id LIMIT 2", Long.class, ana.id());
        // One row from before encryption existed, one in the V30 format without a key id.
        jdbc.update("UPDATE transaction SET description = 'Texto antigo' WHERE id = ?", ids.get(0));
        String current = com.github.yildizmy.security.FieldCipher.encrypt("Formato v1");
        jdbc.update("UPDATE transaction SET description = ? WHERE id = ?",
                "enc:v1:" + current.substring(current.lastIndexOf(':') + 1), ids.get(1));

        var result = rotation.rewrite();

        assertTrue(result.rewritten() >= 2, result.toString());
        assertEquals(0, result.failed());
        for (long id : ids) {
            String stored = jdbc.queryForObject("SELECT description FROM transaction WHERE id = ?", String.class, id);
            assertTrue(com.github.yildizmy.security.FieldCipher.isCurrent(stored), stored);
        }
        assertEquals(java.util.List.of("Texto antigo", "Formato v1"), ids.stream().map(id -> com.github.yildizmy.security.FieldCipher
                .decrypt(jdbc.queryForObject("SELECT description FROM transaction WHERE id = ?", String.class, id))).toList());
        assertEquals(0, rotation.rewrite().rewritten(), "a second run has nothing left to do");
    }

    @Autowired
    private com.github.yildizmy.service.RetentionService retention;

    @Test
    void retentionDeletesOnlyWhatIsPastItsPeriod() throws Exception {
        Session ana = verifiedUser("10.7.0.1");
        long id = ana.id();
        jdbc.update("INSERT INTO forecast_snapshot (user_id, taken_on, balance, projection, updated_at) VALUES "
                + "(?, current_date - 401, 10, '[]', now()), (?, current_date - 399, 20, '[]', now())", id, id);
        jdbc.update("INSERT INTO admin_audit (actor_id, method, path, status, at) VALUES "
                + "(?, 'GET', '/old', 200, now() - interval '366 days'), (?, 'GET', '/new', 200, now() - interval '364 days')", id, id);
        jdbc.update("UPDATE email_verification_challenge SET created_at = now() - interval '31 days' WHERE user_id = ?", id);

        retention.purge();

        assertEquals(java.util.List.of(new java.math.BigDecimal("20.00")), jdbc.queryForList(
                "SELECT balance FROM forecast_snapshot WHERE user_id = ?", java.math.BigDecimal.class, id));
        assertEquals(java.util.List.of("/new"), jdbc.queryForList(
                "SELECT path FROM admin_audit WHERE actor_id = ?", String.class, id));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM email_verification_challenge WHERE user_id = ?", Long.class, id));
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
