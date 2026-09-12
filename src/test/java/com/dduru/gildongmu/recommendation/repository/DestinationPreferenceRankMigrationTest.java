package com.dduru.gildongmu.recommendation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationPreferenceRankMigrationTest {
    @Test
    void migrationEnforcesRequiredBoundedAndUniqueRank() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:rank_migration;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE user_recommendation_destination_preferences (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL)");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V30__destination_preference_rank.sql"));
            statement.execute("INSERT INTO user_recommendation_destination_preferences VALUES (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 2, 1)");
            for (String rank : new String[]{"NULL", "0", "4", "1"}) {
                assertThatThrownBy(() -> statement.execute(
                        "INSERT INTO user_recommendation_destination_preferences VALUES (5, 1, " + rank + ")"))
                        .isInstanceOf(SQLException.class);
            }
        }
    }
}
