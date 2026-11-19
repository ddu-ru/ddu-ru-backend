package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.common.config.SecurityConfig;
import com.dduru.gildongmu.common.jwt.JwtAccessDeniedHandler;
import com.dduru.gildongmu.common.jwt.JwtAuthenticationEntryPoint;
import com.dduru.gildongmu.common.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostShareSecurityIntegrationTest.TestShareController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        PostShareSecurityIntegrationTest.TestShareController.class
})
@DisplayName("게시글 공유 Security 통합 테스트")
class PostShareSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("비회원은 게시글 공유 페이지에 접근할 수 있다")
    void guestCanAccessPostSharePage() throws Exception {
        mockMvc.perform(get("/share/posts/101"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원은 assetlinks.json에 접근할 수 있다")
    void guestCanAccessAssetLinks() throws Exception {
        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestShareController {

        @GetMapping("/share/posts/{postId}")
        ResponseEntity<Void> postShare(@PathVariable Long postId) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/.well-known/assetlinks.json")
        ResponseEntity<Void> assetLinks() {
            return ResponseEntity.ok().build();
        }
    }

    @SpringBootConfiguration
    static class TestApplication {
    }
}
