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
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    @DisplayName("게시글·사용자·메시지 신고와 처리 결과 알림 스키마가 존재한다")
    void reportTargetsAndResultNotificationSchemaAreAvailable() {
        assertThat(string("""
                SELECT IS_NULLABLE FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'reports' AND column_name = 'post_id'
                """)).isEqualTo("YES");
        assertThat(count("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'reports'
                AND column_name IN ('target_type', 'reported_user_id', 'chat_room_id', 'message_id', 'target_snapshot', 'evidence_key')
                """)).isEqualTo(6);
        assertThat(count("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND table_name = 'reports'
                AND constraint_name = 'chk_reports_target' AND constraint_type = 'CHECK'
                """)).isEqualTo(1);
        assertThat(string("""
                SELECT CHECK_CLAUSE FROM information_schema.check_constraints
                WHERE constraint_schema = DATABASE() AND constraint_name = 'chk_reports_target'
                """)).contains("POST", "USER", "CHAT_MESSAGE").doesNotContain("CHAT_ROOM");
        assertThat(string("""
                SELECT COLUMN_TYPE FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'type'
                """)).contains("REPORT_REVIEWED");
        assertThat(string("""
                SELECT COLUMN_TYPE FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'resource_type'
                """)).contains("REPORT");
    }
}
