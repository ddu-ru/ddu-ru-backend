package com.dduru.gildongmu.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("dduru")
            .withUsername("dduru")
            .withPassword("dduru");

    @Test
    void cleanDatabaseStartsFromB32WithRequiredReferenceData() throws Exception {
        var baselineFlyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .target("32")
                .load();

        baselineFlyway.migrate();
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX uk_super_host_tickets_user_source ON super_host_tickets (user_id, source)");
            statement.execute("INSERT INTO users (id, notification_enabled, created_at, modified_at, name, oauth_id, email, oauth_type, role) VALUES (999999, b'1', NOW(6), NOW(6), 'migration-user', 'migration-user', 'migration-user@dduru.test', 'KAKAO', 'USER')");
            statement.execute("INSERT INTO super_host_tickets (user_id, source, status, boost_duration_days, used_at, created_at, modified_at) VALUES (999999, 'ONBOARDING_SURVEY', 'USED', 3, NOW(6), NOW(6), NOW(6))");
        }

        var flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
        flyway.migrate();

        assertThat(flyway.info().pending()).isEmpty();
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '32' AND type = 'SQL_BASELINE' AND success = 1"))
                    .isEqualTo(1);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '33' AND success = 1"))
                    .isEqualTo(1);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'super_host_tickets' AND index_name = 'uk_super_host_tickets_user_source'"))
                    .isZero();
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM super_host_tickets WHERE user_id = 999999 AND source = 'ONBOARDING_SURVEY'"))
                    .isEqualTo(3);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM super_host_tickets WHERE user_id = 999999 AND source = 'ONBOARDING_SURVEY' AND status = 'UNUSED'"))
                    .isEqualTo(2);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM bg_colors")).isGreaterThanOrEqualTo(15);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM destinations")).isEqualTo(54);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM avatar_profiles")).isGreaterThanOrEqualTo(16);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM survey_questions")).isGreaterThanOrEqualTo(14);
            assertThat(singleInt(statement, "SELECT COUNT(*) FROM survey_question_options")).isGreaterThanOrEqualTo(35);
        }
    }

    private int singleInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }
}
