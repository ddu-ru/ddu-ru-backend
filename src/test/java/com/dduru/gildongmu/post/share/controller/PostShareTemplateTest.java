package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.post.share.dto.PostShareView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("게시글 공유 HTML 템플릿 테스트")
class PostShareTemplateTest {

    @Test
    @DisplayName("Open Graph와 canonical 메타데이터를 출력하고 동적 문자열을 escape한다")
    void rendersEscapedOpenGraphMetadata() {
        PostShareView post = new PostShareView(
                101L,
                "제주 <동행> & 여행",
                "제주도 서쪽을 천천히 여행해요.",
                "https://cdn.example.com/post.jpg",
                "제주",
                "2026. 7. 10. - 2026. 7. 12.",
                "2박 3일",
                2,
                4,
                "모집 중",
                "전체 동행",
                "20-29세 · 성별 무관",
                "2026.06.29",
                128,
                java.util.List.of("힐링", "맛집"),
                "제주 & 서귀포 · 2/4명"
        );
        Context context = new Context();
        context.setVariable("post", post);
        context.setVariable("canonicalUrl", "https://api.dduru.app/share/posts/101");
        context.setVariable("playStoreUrl", "");
        context.setVariable("hasPlayStoreUrl", false);

        String html = templateEngine().process("share/post-detail", context);

        assertThat(html).contains("content=\"제주 &amp; 서귀포 · 2/4명\"");
        assertThat(html).contains("content=\"제주 &lt;동행&gt; &amp; 여행\"");
        assertThat(html).contains("rel=\"canonical\" href=\"https://api.dduru.app/share/posts/101\"");
        assertThat(html).contains("여행 소개", "핵심 정보", "해시태그", "전체 동행", "20-29세 · 성별 무관");
        assertThat(html).contains("앱 출시 준비 중");
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
