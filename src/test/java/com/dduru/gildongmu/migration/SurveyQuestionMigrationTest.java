package com.dduru.gildongmu.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class SurveyQuestionMigrationTest {

    private static final String ORDER_MIGRATION = "V36__update_survey_question_display_order.sql";
    private static final List<String> QUESTION_IDS = List.of(
            "rhythm_q1", "rhythm_q2", "rhythm_q3",
            "consumption_q1", "consumption_q2", "consumption_q3",
            "energy_q1", "energy_q2", "energy_q3",
            "decision_q1", "decision_q2", "decision_q3",
            "record_style", "activity_tags"
    );

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("survey_migration")
            .withUsername("dduru")
            .withPassword("dduru");

    @BeforeEach
    void prepareExistingDatabase() throws Exception {
        Flyway flyway = flyway("35");
        flyway.clean();
        flyway.migrate();
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE survey_questions SET modified_at = '2026-10-12 15:04:18.061399'");
        }
    }

    @Test
    void migratesLegacyOrderAndOnlyTouchesChangedQuestions() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            var originalQuestions = snapshot(statement, "SELECT * FROM survey_questions ORDER BY question_id");
            var originalOptions = snapshot(statement, "SELECT * FROM survey_question_options ORDER BY id");

            flyway("36").migrate();

            assertQuestionOrder(statement);
            var updatedQuestions = snapshot(statement, "SELECT * FROM survey_questions ORDER BY question_id");
            for (int i = 0; i < originalQuestions.size(); i++) {
                var original = originalQuestions.get(i);
                var updated = updatedQuestions.get(i);
                if (original.get("display_order").equals(updated.get("display_order"))) {
                    assertThat(updated).isEqualTo(original);
                } else {
                    assertThat(updated.get("modified_at")).isNotEqualTo(original.get("modified_at"));
                    var expected = new LinkedHashMap<>(original);
                    expected.put("display_order", updated.get("display_order"));
                    expected.put("modified_at", updated.get("modified_at"));
                    assertThat(updated).isEqualTo(expected);
                }
            }
            assertThat(snapshot(statement, "SELECT * FROM survey_question_options ORDER BY id"))
                    .isEqualTo(originalOptions);

            executeMigration(connection, ORDER_MIGRATION);
            assertThat(snapshot(statement, "SELECT * FROM survey_questions ORDER BY question_id"))
                    .isEqualTo(updatedQuestions);
        }
    }

    @Test
    void preservesManuallyAppliedOrderIncludingModificationTimes() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            applyManualOrder(connection);
            var before = snapshot(statement, "SELECT * FROM survey_questions ORDER BY question_id");

            flyway("36").migrate();

            assertQuestionOrder(statement);
            assertThat(snapshot(statement, "SELECT * FROM survey_questions ORDER BY question_id"))
                    .isEqualTo(before);
        }
    }

    private void applyManualOrder(Connection connection) throws Exception {
        try (var update = connection.prepareStatement(
                "UPDATE survey_questions SET display_order = ? WHERE question_id = ?")) {
            for (int i = 0; i < QUESTION_IDS.size(); i++) {
                update.setInt(1, i + 1);
                update.setString(2, QUESTION_IDS.get(i));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void assertQuestionOrder(Statement statement) throws Exception {
        try (ResultSet rows = statement.executeQuery(
                "SELECT question_id, display_order FROM survey_questions ORDER BY display_order")) {
            for (int i = 0; i < QUESTION_IDS.size(); i++) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("question_id")).isEqualTo(QUESTION_IDS.get(i));
                assertThat(rows.getInt("display_order")).isEqualTo(i + 1);
            }
            assertThat(rows.next()).isFalse();
        }
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .target(target)
                .load();
    }

    private void executeMigration(Connection connection, String filename) {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/" + filename));
    }

    private List<Map<String, Object>> snapshot(Statement statement, String sql) throws Exception {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery(sql)) {
            var metadata = rows.getMetaData();
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    row.put(metadata.getColumnLabel(column), rows.getObject(column));
                }
                snapshot.add(row);
            }
        }
        return snapshot;
    }
}
