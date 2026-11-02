package com.dduru.gildongmu.verification.service;

import com.dduru.gildongmu.common.exception.InternalServerException;
import com.dduru.gildongmu.common.time.KoreaTime;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.verification.dto.VerificationData;
import com.dduru.gildongmu.verification.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class VerificationRedisIntegrationTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, String> redis;
    private final ObjectMapper mapper = new ObjectMapper();
    private VerificationCodeService service;
    private static final String PHONE = "01011112222";
    private static final String AUTH = "sms:auth:" + PHONE;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new RedisTemplate<>();
        redis.setConnectionFactory(connectionFactory);
        redis.setKeySerializer(new StringRedisSerializer());
        redis.setValueSerializer(new StringRedisSerializer());
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redis.delete(redis.keys("sms:*"));
        service = new VerificationCodeService(redis, mapper, new TimeProvider(Clock.system(KoreaTime.ZONE_ID)));
        service.init();
    }

    @Test
    void responseAndRedisHaveExactlyTheSameExpiration() {
        var result = service.createVerificationWithAdminCode(PHONE, "123456");
        assertThat(expiration(AUTH)).isEqualTo(result.expiresAt().atZone(KoreaTime.ZONE_ID).toInstant().toEpochMilli());
        assertThat(redis.getExpire(AUTH, TimeUnit.MILLISECONDS)).isBetween(175_000L, 180_000L);
    }

    @Test
    void wrongCodePreservesOriginalExpiration() throws Exception {
        createCode();
        redis.expire(AUTH, Duration.ofSeconds(20));
        long originalExpiry = expiration(AUTH);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(expiration(AUTH)).isEqualTo(originalExpiry);
        assertThat(data().count()).isEqualTo(1);
    }

    @Test
    void expiredCodeIsNotRecreated() {
        createCode();
        redis.expire(AUTH, Duration.ZERO);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "123456")).isInstanceOf(VerificationNotFoundException.class);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(VerificationNotFoundException.class);
        assertThat(redis.hasKey(AUTH)).isFalse();
    }

    @Test
    void codeWithoutExpirationFailsClosed() {
        createCode();
        redis.persist(AUTH);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "123456")).isInstanceOf(InternalServerException.class);
    }

    @Test
    void concurrentWrongCodesDoNotLoseAttemptsOrExtendExpiry() throws Exception {
        createCode();
        long originalExpiry = expiration(AUTH);
        var results = concurrently(5, "000000");
        assertThat(results).allMatch(result -> result.equals("InvalidVerificationCodeException"));
        assertThat(data().count()).isEqualTo(5);
        assertThat(expiration(AUTH)).isEqualTo(originalExpiry);
        assertThatThrownBy(() -> service.verifyCode(PHONE, "000000")).isInstanceOf(VerificationAttemptsExceededException.class);
        assertThat(redis.hasKey(AUTH)).isFalse();
    }

    @Test
    void concurrentCorrectCodesVerifyOnlyOnceAndKeepVerifiedForTenMinutes() throws Exception {
        createCode();
        assertThat(concurrently(2, "123456")).containsExactlyInAnyOrder("verified", "AlreadyVerifiedException");
        assertThat(data().status()).isEqualTo("VERIFIED");
        assertThat(redis.getExpire(AUTH, TimeUnit.MILLISECONDS)).isBetween(595_000L, 600_000L);
    }

    private void createCode() {
        service.createVerificationWithAdminCode(PHONE, "123456");
    }

    private VerificationData data() throws Exception {
        return mapper.readValue(redis.opsForValue().get(AUTH), VerificationData.class);
    }

    private long expiration(String key) {
        return redis.execute(new DefaultRedisScript<>("return redis.call('PEXPIRETIME', KEYS[1])", Long.class), List.of(key));
    }

    private List<String> concurrently(int count, String code) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try {
            var futures = IntStream.range(0, count).mapToObj(ignored -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    service.verifyCode(PHONE, code);
                    return "verified";
                } catch (com.dduru.gildongmu.common.exception.BusinessException exception) {
                    return exception.getClass().getSimpleName();
                }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new java.util.ArrayList<String>();
            for (var future : futures) results.add(future.get(10, TimeUnit.SECONDS));
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
