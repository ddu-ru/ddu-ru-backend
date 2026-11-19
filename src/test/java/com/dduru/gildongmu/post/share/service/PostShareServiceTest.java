package com.dduru.gildongmu.post.share.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.post.share.dto.PostShareView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostShareService 테스트")
class PostShareServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private JsonConverter jsonConverter;

    @InjectMocks
    private PostShareService postShareService;

    @Test
    @DisplayName("공유 페이지용 게시글 정보를 조회해도 조회 수는 증가하지 않는다")
    void retrievesShareViewWithoutIncrementingViewCount() {
        Long postId = 101L;
        LocalDate today = LocalDate.of(2026, 7, 1);
        Post post = createPost(postId);
        when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
        when(timeProvider.today()).thenReturn(today);
        when(jsonConverter.convertJsonToList("[]")).thenReturn(java.util.List.of("힐링", "맛집"));

        PostShareView result = postShareService.getShareView(postId);

        assertThat(result.postId()).isEqualTo(postId);
        assertThat(result.title()).isEqualTo("제주도 2박 3일 동행 구해요");
        assertThat(result.destination()).isEqualTo("제주");
        assertThat(result.travelPeriod()).isEqualTo("2026. 7. 10. - 2026. 7. 12.");
        assertThat(result.tripDuration()).isEqualTo("2박 3일");
        assertThat(result.recruitmentStatus()).isEqualTo("모집 중");
        assertThat(result.companionType()).isEqualTo("전체 동행");
        assertThat(result.ageCondition()).isEqualTo("연령 무관 · 성별 무관");
        assertThat(result.tags()).containsExactly("힐링", "맛집");
        verify(postRepository, never()).incrementViewCount(postId);
    }

    private Post createPost(Long postId) {
        Destination destination = Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .image("https://cdn.example.com/jeju.jpg")
                .build();
        Post post = Post.createPost(
                null,
                destination,
                "제주도 2박 3일 동행 구해요",
                "제주도 서쪽 위주로 천천히 여행하실 분을 찾습니다.",
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 12),
                4,
                LocalDate.of(2026, 7, 9),
                com.dduru.gildongmu.profile.domain.enums.Gender.U,
                true,
                null,
                null,
                "https://cdn.example.com/post.jpg",
                "[]",
                com.dduru.gildongmu.post.domain.enums.CompanionType.FULL
        );
        org.springframework.test.util.ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
