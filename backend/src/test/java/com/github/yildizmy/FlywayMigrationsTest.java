package com.github.yildizmy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Applies every migration to an empty PostgreSQL and checks the rules that live only in the database.
 * The unit tests mock the repositories, so without this a broken migration only shows up at deploy.
 * Runs when WALLIA_IT_DB_URL points to an empty database (always in CI; locally with Docker, see the guide).
 */
@EnabledIfEnvironmentVariable(named = "WALLIA_IT_DB_URL", matches = ".+")
class FlywayMigrationsTest {

    private static final String URL = System.getenv("WALLIA_IT_DB_URL");
    private static final String USER = System.getenv().getOrDefault("WALLIA_IT_DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("WALLIA_IT_DB_PASSWORD", "postgres");

    @BeforeAll
    static void migrate() {
        Flyway flyway = Flyway.configure().dataSource(URL, USER, PASSWORD).cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void everyMigrationAppliesOnAnEmptyDatabase() throws SQLException {
        // The highest V<n>__ file on disk, so a new migration does not need this test edited.
        String latest = String.valueOf(java.util.Arrays.stream(
                        new java.io.File("src/main/resources/db/migration").list((dir, name) -> name.matches("V[0-9]+__.*[.]sql")))
                .mapToInt(name -> Integer.parseInt(name.substring(1, name.indexOf("__"))))
                .max().orElseThrow());
        assertEquals(latest, scalar("SELECT max(version::int)::text FROM flyway_schema_history WHERE success"));
        assertEquals("0", scalar("SELECT count(*)::text FROM flyway_schema_history WHERE NOT success"));
    }

    @Test
    void accountsThatExistedBeforeSignupVerificationCanStillLogIn() throws SQLException {
        assertEquals("0", scalar("SELECT count(*)::text FROM \"user\" WHERE email_verified_at IS NULL AND id < 9000"),
                "rows other tests insert later (id 9000+) are not part of the migration");
    }

    @Test
    void consentStatusOnlyAcceptsTheOpenFinanceValues() throws SQLException {
        assertEquals("AUTHORISED,AWAITING_AUTHORISATION,REJECTED", scalar("""
                SELECT string_agg(value, ',' ORDER BY value) FROM (
                  SELECT unnest(regexp_matches(pg_get_constraintdef(oid), '''([A-Z_]+)''', 'g')) AS value
                  FROM pg_constraint WHERE conname = 'open_finance_connection_status_check') AS found
                """));
    }

    @Test
    void theTemplateDemoAccountsCannotLogInAndAreNotAdmins() throws SQLException {
        assertEquals("0", scalar("""
                SELECT count(*)::text FROM "user" WHERE username IN ('johndoe','lindacalvin','jeffreytaylor')
                AND password <> '{disabled}'"""));
        assertEquals("0", scalar("""
                SELECT count(*)::text FROM user_role ur JOIN role r ON r.id = ur.role_id
                JOIN "user" u ON u.id = ur.user_id
                WHERE r.type = 'ROLE_ADMIN' AND u.username IN ('johndoe','lindacalvin','jeffreytaylor')"""));
    }

    @Test
    void usernamesAndEmailsThatDifferOnlyByCaseAreRejected() throws SQLException {
        execute("""
                INSERT INTO "user" (id, first_name, last_name, username, email, password)
                VALUES (9001, 'Gui', 'Teste', 'gui_case', 'gui@case.test', 'x')""");
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO "user" (id, first_name, last_name, username, email, password)
                VALUES (9002, 'Gui', 'Teste', 'GUI_CASE', 'other@case.test', 'x')"""));
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO "user" (id, first_name, last_name, username, email, password)
                VALUES (9003, 'Gui', 'Teste', 'gui_other', 'GUI@CASE.TEST', 'x')"""));
    }

    @Test
    void moneyRulesHoldEvenIfTheApplicationForgetsThem() throws SQLException {
        execute("""
                INSERT INTO "user" (id, first_name, last_name, username, email, password)
                VALUES (9101, 'Ana', 'Teste', 'ana_money', 'ana@money.test', 'x')""");
        assertThrows(SQLException.class, () -> execute(
                "INSERT INTO envelope (user_id, name, amount, created_at) VALUES (9101, 'Zero', 0, now())"));
        assertThrows(SQLException.class, () -> execute(
                "INSERT INTO goal (user_id, name, monthly_amount, day_of_month, created_at) VALUES (9101, 'Meta', 10, 32, now())"));
        execute("INSERT INTO forecast_snapshot (user_id, taken_on, balance, updated_at) VALUES (9101, current_date, 1, now())");
        assertThrows(SQLException.class, () -> execute(
                "INSERT INTO forecast_snapshot (user_id, taken_on, balance, updated_at) VALUES (9101, current_date, 2, now())"));
        execute("""
                INSERT INTO push_subscription (user_id, endpoint, p256dh, auth, created_at)
                VALUES (9101, 'https://fcm.googleapis.com/x', 'k', 'a', now())""");
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO push_subscription (user_id, endpoint, p256dh, auth, created_at)
                VALUES (9101, 'https://fcm.googleapis.com/x', 'k', 'a', now())"""));
    }

    private static String scalar(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
