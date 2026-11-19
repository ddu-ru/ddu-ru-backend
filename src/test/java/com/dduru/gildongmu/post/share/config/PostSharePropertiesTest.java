package com.dduru.gildongmu.post.share.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostShareProperties 테스트")
class PostSharePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("쉼표로 구분된 Android 인증서 지문을 리스트로 바인딩한다")
    void bindsCommaSeparatedFingerprints() {
        contextRunner
                .withPropertyValues(
                        "app.share.android-cert-fingerprints=AA:BB, CC:DD",
                        "app.share.base-url=https://share.example.com/"
                )
                .run(context -> {
                    PostShareProperties properties = context.getBean(PostShareProperties.class);

                    assertThat(properties.normalizedAndroidCertFingerprints())
                            .containsExactly("AA:BB", "CC:DD");
                    assertThat(properties.postShareUrl(101L))
                            .isEqualTo("https://share.example.com/share/posts/101");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PostShareProperties.class)
    static class TestConfiguration {
    }
}
