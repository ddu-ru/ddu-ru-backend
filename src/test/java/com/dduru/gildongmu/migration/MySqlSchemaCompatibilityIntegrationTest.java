package com.dduru.gildongmu.migration;

import com.dduru.gildongmu.support.mysql.MySqlIntegrationTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MySQL 운영 스키마 호환성 통합 테스트")
class MySqlSchemaCompatibilityIntegrationTest extends MySqlIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("MySQL 8.4.9에 최신 Flyway를 적용하고 REPEATABLE-READ와 JPA 매핑을 검증한다")
    void validatesProductionCompatibleSchema() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        String isolation = jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class);

        assertThat(version).startsWith("8.4.9");
        assertThat(isolation).isEqualToIgnoringCase("REPEATABLE-READ");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("34");
    }
}
