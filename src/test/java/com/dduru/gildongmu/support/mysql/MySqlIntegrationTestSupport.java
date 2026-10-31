package com.dduru.gildongmu.support.mysql;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.fcm.service.FcmPushService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

import static org.mockito.Mockito.when;

@Tag("mysql-integration")
@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class MySqlIntegrationTestSupport {

    protected static final String MYSQL_IMAGE = "mysql:8.4.9";
    protected static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 31, 12, 0);
    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Seoul");

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("dduru")
            .withUsername("dduru")
            .withPassword("dduru");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected FcmPushService fcmPushService;

    @MockitoBean
    protected TimeProvider timeProvider;

    @BeforeEach
    void setUpFixedTime() {
        Instant fixedInstant = FIXED_NOW.atZone(TEST_ZONE).toInstant();
        when(timeProvider.now()).thenReturn(FIXED_NOW);
        when(timeProvider.today()).thenReturn(FIXED_NOW.toLocalDate());
        when(timeProvider.instant()).thenReturn(fixedInstant);
        when(timeProvider.zoneId()).thenReturn(TEST_ZONE);
    }

    protected <T> T committed(Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> work.get());
    }

    protected void committed(Runnable work) {
        committed(() -> {
            work.run();
            return null;
        });
    }

    protected <T> T queryCommitted(Supplier<T> query) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setReadOnly(true);
        return transaction.execute(status -> {
            entityManager.clear();
            T result = query.get();
            entityManager.clear();
            return result;
        });
    }

    protected long count(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return value == null ? 0 : value;
    }

    protected int integer(String sql, Object... arguments) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return value == null ? 0 : value;
    }

    protected String string(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, String.class, arguments);
    }

    protected Long longValue(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, Long.class, arguments);
    }
}
