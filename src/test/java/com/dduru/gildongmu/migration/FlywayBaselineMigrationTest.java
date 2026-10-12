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
