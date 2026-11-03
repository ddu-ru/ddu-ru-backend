package com.dduru.gildongmu.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyRetakeMigrationTest {
    @Test
    void grantsImmediateRetestToAllExistingAndNewUsers() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:survey_retake_migration;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE surveys (id BIGINT PRIMARY KEY, created_at TIMESTAMP(6), modified_at TIMESTAMP(6))");
            statement.execute("""
                    INSERT INTO surveys VALUES
                    (1, '2026-10-01 10:00:00', '2026-10-01 10:00:00'),
                    (2, '2026-10-01 10:00:00', '2026-10-15 12:00:00'),
                    (3, '2026-10-01 10:00:00', '2026-10-01 10:00:01')
                    """);
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V38__add_survey_last_retested_at.sql"));
            statement.execute("INSERT INTO surveys (id, created_at, modified_at) VALUES (4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            try (var result = statement.executeQuery("SELECT last_retested_at FROM surveys ORDER BY id")) {
                for (int i = 0; i < 4; i++) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getDate(1)).isNull();
                }
                assertThat(result.next()).isFalse();
            }
            try (var result = statement.executeQuery("SELECT modified_at FROM surveys WHERE id = 2")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getTimestamp(1).toLocalDateTime().toString()).isEqualTo("2026-10-15T12:00");
            }
        }
    }
}
