package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.post.exception.PostNotFoundException;
import com.dduru.gildongmu.post.share.config.PostShareProperties;
import com.dduru.gildongmu.post.share.dto.PostShareView;
import com.dduru.gildongmu.post.share.service.PostShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@DisplayName("PostShareController 테스트")
class PostShareControllerTest {

    private PostShareService postShareService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postShareService = mock(PostShareService.class);
        PostShareProperties properties = new PostShareProperties();
        properties.setBaseUrl("https://api.dduru.app/");
        mockMvc = standaloneSetup(new PostShareController(postShareService, properties)).build();
    }

    @Test
    @DisplayName("활성 게시글은 공유 상세 HTML 모델을 반환한다")
    void retrievesPostSharePage() throws Exception {
        PostShareView post = new PostShareView(
                101L,
                "제주도 동행 구해요",
                "제주도 서쪽을 함께 여행해요.",
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
                "제주 · 2026. 7. 10. - 2026. 7. 12. · 2/4명 · 모집 중"
        );
        when(postShareService.getShareView(101L)).thenReturn(post);

        mockMvc.perform(get("/share/posts/101"))
                .andExpect(status().isOk())
                .andExpect(view().name("share/post-detail"))
                .andExpect(model().attribute("post", post))
                .andExpect(model().attribute("canonicalUrl", "https://api.dduru.app/share/posts/101"))
                .andExpect(model().attribute("hasPlayStoreUrl", false));
    }

    @Test
    @DisplayName("삭제되었거나 없는 게시글은 전용 HTML과 404를 반환한다")
    void returnsHtmlNotFoundPage() throws Exception {
        when(postShareService.getShareView(999L)).thenThrow(new PostNotFoundException());

        mockMvc.perform(get("/share/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("share/post-not-found"));
    }
}
