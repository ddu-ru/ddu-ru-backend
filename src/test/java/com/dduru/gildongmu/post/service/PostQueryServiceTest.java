package com.dduru.gildongmu.post.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.like.domain.PostLike;
import com.dduru.gildongmu.like.repository.PostLikeRepository;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.MyPagePostFilter;
import com.dduru.gildongmu.post.domain.enums.PostSortType;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.dto.request.MyPageLikedPostListRequest;
import com.dduru.gildongmu.post.dto.request.MyPagePostListRequest;
import com.dduru.gildongmu.post.dto.request.PostListRequest;
import com.dduru.gildongmu.post.dto.response.MyPageLikedPostListResponse;
import com.dduru.gildongmu.post.dto.response.MyPagePostDisplayStatus;
import com.dduru.gildongmu.post.dto.response.MyPagePostListResponse;
import com.dduru.gildongmu.post.dto.response.PostListResponse;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.superhost.service.SuperHostService;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostQueryService 테스트")
class PostQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 5);

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private SuperHostService superHostService;
    @Mock private TimeProvider timeProvider;

    @InjectMocks
    private PostQueryService postQueryService;

    @BeforeEach
    void setUp() {
        lenient().when(timeProvider.today()).thenReturn(TODAY);
        lenient().when(superHostService.findActiveSuperHostExposures(any())).thenReturn(Map.of());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // hasLiked
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasLiked")
    class HasLiked {

        @Test
        @DisplayName("비로그인 사용자는 hasLiked가 항상 false이고 좋아요 조회를 하지 않는다")
        void nonLoggedInUserHasLikedAlwaysFalse() {
            Post post = createPost(1L);
            PostListRequest request = listRequest(2, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(post));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.posts()).hasSize(1);
            assertThat(response.posts().get(0).hasLiked()).isFalse();
            verify(postLikeRepository, never()).findLikedPostIdsByUserId(any(), any());
        }

        @Test
        @DisplayName("로그인 사용자는 좋아요한 게시글의 hasLiked가 true다")
        void loggedInUserHasLikedTrueForLikedPost() {
            Post likedPost = createPost(1L);
            Post notLikedPost = createPost(2L);
            Long userId = 10L;
            PostListRequest request = listRequest(5, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(likedPost, notLikedPost));
            when(postLikeRepository.findLikedPostIdsByUserId(eq(userId), any()))
                    .thenReturn(Set.of(1L));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, userId);

            assertThat(response.posts()).hasSize(2);
            assertThat(response.posts().get(0).hasLiked()).isTrue();
            assertThat(response.posts().get(1).hasLiked()).isFalse();
        }

        @Test
        @DisplayName("로그인 사용자여도 게시글이 없으면 좋아요 조회를 하지 않는다")
        void emptyPostsSkipsLikeQuery() {
            Long userId = 10L;
            PostListRequest request = listRequest(5, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveAllWithFilter(request, userId);

            verify(postLikeRepository, never()).findLikedPostIdsByUserId(any(), any());
            verify(superHostService, never()).findActiveSuperHostExposures(any());
        }
    }

    @Nested
    @DisplayName("목록 작성자 정보")
    class SummaryAuthor {

        @Test
        @DisplayName("게시글 목록은 작성자 닉네임과 슈퍼호스트 여부를 반환한다")
        void returnsSummaryAuthorInfo() {
            Post post = createPost(1L);
            PostListRequest request = listRequest(5, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(post));
            when(superHostService.findActiveSuperHostExposures(List.of(1L)))
                    .thenReturn(Map.of(1L, LocalDateTime.of(2026, 5, 6, 0, 0)));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.posts().get(0).author().nickname()).isEqualTo("닉네임1");
            assertThat(response.posts().get(0).author().isSuperHost()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 페이지네이션
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("페이지네이션")
    class Pagination {

        @Test
        @DisplayName("조회 결과가 size보다 많으면 hasNext가 true이고 size개만 반환된다")
        void hasNextTrueWhenMoreThanSize() {
            int size = 2;
            PostListRequest request = listRequest(size, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(createPost(1L), createPost(2L), createPost(3L)));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isTrue();
            assertThat(response.posts()).hasSize(size);
        }

        @Test
        @DisplayName("조회 결과가 size 이하이면 hasNext가 false다")
        void hasNextFalseWhenLessThanOrEqualSize() {
            int size = 2;
            PostListRequest request = listRequest(size, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(createPost(1L), createPost(2L)));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isFalse();
            assertThat(response.posts()).hasSize(2);
        }

        @Test
        @DisplayName("LATEST 정렬에서 hasNext가 true이면 nextCursor는 마지막 게시글의 ID이고 nextCursorValue는 null이다")
        void nextCursorIsLastPostIdWhenHasNext() {
            int size = 2;
            PostListRequest request = listRequest(size, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(createPost(10L), createPost(5L), createPost(1L)));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(5L);
            assertThat(response.nextCursorValue()).isNull();
        }

        @Test
        @DisplayName("VIEW 정렬에서 hasNext가 true이면 nextCursorValue는 마지막 게시글의 viewCount다")
        void nextCursorValueIsLastPostViewCountForViewSort() {
            int size = 2;
            PostListRequest request = listRequestWithSort(size, null, null, PostSortType.VIEW);
            Post post1 = createPost(10L);
            Post post2 = createPost(5L);
            Post post3 = createPost(1L);
            ReflectionTestUtils.setField(post1, "viewCount", 100);
            ReflectionTestUtils.setField(post2, "viewCount", 50);
            ReflectionTestUtils.setField(post3, "viewCount", 20);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(post1, post2, post3));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(5L);
            assertThat(response.nextCursorValue()).isEqualTo(50);
        }

        @Test
        @DisplayName("LIKE 정렬에서 hasNext가 true이면 nextCursorValue는 마지막 게시글의 likeCount다")
        void nextCursorValueIsLastPostLikeCountForLikeSort() {
            int size = 2;
            PostListRequest request = listRequestWithSort(size, null, null, PostSortType.LIKE);
            Post post1 = createPost(10L);
            Post post2 = createPost(5L);
            Post post3 = createPost(1L);
            ReflectionTestUtils.setField(post1, "likeCount", 30);
            ReflectionTestUtils.setField(post2, "likeCount", 15);
            ReflectionTestUtils.setField(post3, "likeCount", 5);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(post1, post2, post3));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(5L);
            assertThat(response.nextCursorValue()).isEqualTo(15);
        }

        @Test
        @DisplayName("hasNext가 false이면 nextCursorValue는 항상 null이다")
        void nextCursorValueIsNullWhenNoNextPage() {
            int size = 2;
            PostListRequest request = listRequestWithSort(size, null, null, PostSortType.VIEW);
            Post post1 = createPost(10L);
            ReflectionTestUtils.setField(post1, "viewCount", 100);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(post1));

            PostListResponse response = postQueryService.retrieveAllWithFilter(request, null);

            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursorValue()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 커서 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("커서 조회")
    class Cursor {

        @Test
        @DisplayName("VIEW 정렬에서 cursor와 cursorValue가 있으면 cursorValue를 레포지토리에 전달한다")
        void viewSortWithCursorPassesCursorValue() {
            Long cursorId = 100L;
            Integer cursorValue = 150;
            PostListRequest request = listRequestWithSort(5, cursorId, cursorValue, PostSortType.VIEW);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), eq(cursorValue), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveAllWithFilter(request, null);

            verify(postRepository, never()).findById(any());
            verify(postRepository).findPostsWithFilters(any(), any(LocalDate.class), eq(cursorValue), any(Pageable.class));
        }

        @Test
        @DisplayName("LATEST 정렬에서는 cursor가 있어도 cursorValue를 사용하지 않는다")
        void latestSortWithCursorPassesNullCursorValue() {
            PostListRequest request = listRequest(5, 100L);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveAllWithFilter(request, null);

            verify(postRepository, never()).findById(any());
            verify(postRepository).findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("cursor가 없으면 cursorValue 없이 조회한다")
        void withoutCursorPassesNullCursorValue() {
            PostListRequest request = listRequest(5, null);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveAllWithFilter(request, null);

            verify(postRepository, never()).findById(any());
            verify(postRepository).findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("VIEW 정렬에서 cursorValue가 없으면 null을 레포지토리에 전달한다")
        void viewSortWithoutCursorValuePassesNull() {
            PostListRequest request = listRequestWithSort(5, null, null, PostSortType.VIEW);

            when(postRepository.findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveAllWithFilter(request, null);

            verify(postRepository, never()).findById(any());
            verify(postRepository).findPostsWithFilters(any(), any(LocalDate.class), isNull(), any(Pageable.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내가 작성한 게시글 - displayStatus 분류
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("displayStatus 분류")
    class DisplayStatus {

        @Test
        @DisplayName("endDate가 오늘 이전이면 TRAVEL_ENDED다")
        void travelEndedWhenEndDateBeforeToday() {
            Post post = createPostWithEndDate(1L, TODAY.minusDays(1));
            stubMyPosts(List.of(post));

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts().get(0).displayStatus()).isEqualTo(MyPagePostDisplayStatus.TRAVEL_ENDED);
        }

        @Test
        @DisplayName("endDate가 오늘이고 OPEN이며 정원이 안 찼으면 RECRUITING이다")
        void recruitingWhenEndDateTodayAndOpenAndNotFull() {
            Post post = createPostWithEndDate(1L, TODAY);
            stubMyPosts(List.of(post));

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts().get(0).displayStatus()).isEqualTo(MyPagePostDisplayStatus.RECRUITING);
        }

        @Test
        @DisplayName("endDate가 미래이고 OPEN이며 정원이 안 찼으면 RECRUITING이다")
        void recruitingWhenOpenAndNotFull() {
            Post post = createPost(1L);
            stubMyPosts(List.of(post));

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts().get(0).displayStatus()).isEqualTo(MyPagePostDisplayStatus.RECRUITING);
        }

        @Test
        @DisplayName("CLOSED 상태이고 endDate가 미래이면 RECRUITMENT_CLOSED다")
        void recruitmentClosedWhenStatusClosed() {
            Post post = createPost(1L);
            ReflectionTestUtils.setField(post, "status", PostStatus.CLOSED);
            stubMyPosts(List.of(post));

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts().get(0).displayStatus()).isEqualTo(MyPagePostDisplayStatus.RECRUITMENT_CLOSED);
        }

        @Test
        @DisplayName("OPEN이지만 정원이 찼으면 RECRUITMENT_CLOSED다")
        void recruitmentClosedWhenFull() {
            Post post = createPost(1L);
            ReflectionTestUtils.setField(post, "recruitCapacity", 2);
            ReflectionTestUtils.setField(post, "recruitCount", 2);
            stubMyPosts(List.of(post));

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts().get(0).displayStatus()).isEqualTo(MyPagePostDisplayStatus.RECRUITMENT_CLOSED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내가 작성한 게시글 - 필터
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("마이페이지 상태 필터")
    class MyPageFilter {

        @Test
        @DisplayName("RECRUITING 필터를 레포지토리에 전달한다")
        void passesRecruitingFilterToRepository() {
            when(postRepository.findPostsByUserId(eq(1L), eq(MyPagePostFilter.RECRUITING), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of());
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(0L);

            postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.RECRUITING));

            verify(postRepository).findPostsByUserId(eq(1L), eq(MyPagePostFilter.RECRUITING), isNull(), any(Pageable.class), eq(TODAY));
        }

        @Test
        @DisplayName("TRAVEL_ENDED 필터를 레포지토리에 전달한다")
        void passesTravelEndedFilterToRepository() {
            when(postRepository.findPostsByUserId(eq(1L), eq(MyPagePostFilter.TRAVEL_ENDED), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of());
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(0L);

            postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.TRAVEL_ENDED));

            verify(postRepository).findPostsByUserId(eq(1L), eq(MyPagePostFilter.TRAVEL_ENDED), isNull(), any(Pageable.class), eq(TODAY));
        }

        @Test
        @DisplayName("ALL 필터를 레포지토리에 전달한다")
        void passesAllFilterToRepository() {
            when(postRepository.findPostsByUserId(eq(1L), eq(MyPagePostFilter.ALL), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of());
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(0L);

            postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            verify(postRepository).findPostsByUserId(eq(1L), eq(MyPagePostFilter.ALL), isNull(), any(Pageable.class), eq(TODAY));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내가 작성한 게시글 - 요약 카드 카운트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("전체 게시글 수")
    class TotalPostCount {

        @Test
        @DisplayName("totalPostCount를 응답에 포함한다")
        void includesTotalPostCountInResponse() {
            when(postRepository.findPostsByUserId(any(), any(), any(), any(), any())).thenReturn(List.of());
            when(postRepository.countTotalPostsByUserId(eq(1L))).thenReturn(4L);

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.totalPostCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("필터와 무관하게 삭제되지 않은 전체 게시글 수를 조회한다")
        void totalCountIsIndependentOfFilter() {
            when(postRepository.findPostsByUserId(any(), any(), any(), any(), any())).thenReturn(List.of());
            when(postRepository.countTotalPostsByUserId(eq(1L))).thenReturn(5L);

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(1L, myPageRequest(null, 10, MyPagePostFilter.TRAVEL_ENDED));

            assertThat(response.totalPostCount()).isEqualTo(5);
            verify(postRepository).countTotalPostsByUserId(eq(1L));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내가 작성한 게시글 - 페이지네이션
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("내가 작성한 게시글")
    class MyPosts {

        @Test
        @DisplayName("결과가 있으면 게시글 목록과 nextCursor를 반환한다")
        void returnsPostsWithNextCursor() {
            Long userId = 1L;
            when(postRepository.findPostsByUserId(eq(userId), eq(MyPagePostFilter.ALL), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of(createPost(10L), createPost(5L), createPost(1L)));
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(3L);

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(userId, myPageRequest(null, 2, MyPagePostFilter.ALL));

            assertThat(response.posts()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(5L);
        }

        @Test
        @DisplayName("결과가 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoPosts() {
            Long userId = 1L;
            when(postRepository.findPostsByUserId(eq(userId), eq(MyPagePostFilter.ALL), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of());
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(0L);

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(userId, myPageRequest(null, 10, MyPagePostFilter.ALL));

            assertThat(response.posts()).isEmpty();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 있으면 레포지토리에 커서를 전달한다")
        void passesCursorToRepository() {
            Long userId = 1L;
            Long cursor = 50L;
            when(postRepository.findPostsByUserId(eq(userId), any(), eq(cursor), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of());
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(0L);

            postQueryService.retrieveMyPosts(userId, myPageRequest(cursor, 10, MyPagePostFilter.ALL));

            verify(postRepository).findPostsByUserId(eq(userId), any(), eq(cursor), any(Pageable.class), eq(TODAY));
        }

        @Test
        @DisplayName("페이지 크기 만큼만 반환하고 마지막 항목의 ID가 nextCursor다")
        void returnsExactSizeAndCorrectNextCursor() {
            Long userId = 1L;
            Post post10 = createPost(10L);
            Post post5 = createPost(5L);
            Post post1 = createPost(1L);
            when(postRepository.findPostsByUserId(eq(userId), any(), isNull(), any(Pageable.class), eq(TODAY)))
                    .thenReturn(List.of(post10, post5, post1));
            lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn(3L);

            MyPagePostListResponse response = postQueryService.retrieveMyPosts(userId, myPageRequest(null, 2, MyPagePostFilter.ALL));

            assertThat(response.posts()).hasSize(2);
            assertThat(response.nextCursor()).isEqualTo(5L);
            assertThat(response.size()).isEqualTo(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 찜한 여행 목록
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("찜한 여행 목록")
    class MyLikedPosts {

        @Test
        @DisplayName("결과가 있으면 찜한 게시글 목록과 nextCursor를 반환한다")
        void returnsLikedPostsWithNextCursor() {
            Long userId = 1L;
            PostLike like10 = createPostLike(10L, createPost(1L));
            PostLike like5 = createPostLike(5L, createPost(2L));
            PostLike like1 = createPostLike(1L, createPost(3L));
            when(postLikeRepository.findLikedPostsByUserIdWithCursor(eq(userId), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(like10, like5, like1));

            MyPageLikedPostListResponse response = postQueryService.retrieveMyLikedPosts(userId, likedPostRequest(null, 2));

            assertThat(response.posts()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(5L);
        }

        @Test
        @DisplayName("결과가 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoLikedPosts() {
            Long userId = 1L;
            when(postLikeRepository.findLikedPostsByUserIdWithCursor(eq(userId), isNull(), any(Pageable.class)))
                    .thenReturn(List.of());

            MyPageLikedPostListResponse response = postQueryService.retrieveMyLikedPosts(userId, likedPostRequest(null, 10));

            assertThat(response.posts()).isEmpty();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 있으면 레포지토리에 커서를 전달한다")
        void passesCursorToRepository() {
            Long userId = 1L;
            Long cursor = 50L;
            when(postLikeRepository.findLikedPostsByUserIdWithCursor(eq(userId), eq(cursor), any(Pageable.class)))
                    .thenReturn(List.of());

            postQueryService.retrieveMyLikedPosts(userId, likedPostRequest(cursor, 10));

            verify(postLikeRepository).findLikedPostsByUserIdWithCursor(eq(userId), eq(cursor), any(Pageable.class));
        }

        @Test
        @DisplayName("페이지 크기만큼만 반환하고 마지막 PostLike ID가 nextCursor다")
        void returnsExactSizeAndCorrectNextCursor() {
            Long userId = 1L;
            PostLike like10 = createPostLike(10L, createPost(1L));
            PostLike like5 = createPostLike(5L, createPost(2L));
            PostLike like1 = createPostLike(1L, createPost(3L));
            when(postLikeRepository.findLikedPostsByUserIdWithCursor(eq(userId), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(like10, like5, like1));

            MyPageLikedPostListResponse response = postQueryService.retrieveMyLikedPosts(userId, likedPostRequest(null, 2));

            assertThat(response.posts()).hasSize(2);
            assertThat(response.nextCursor()).isEqualTo(5L);
            assertThat(response.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("정확히 size개 결과이면 hasNext=false이고 nextCursor=null이다")
        void returnsHasNextFalseWhenExactSize() {
            Long userId = 1L;
            PostLike like5 = createPostLike(5L, createPost(1L));
            PostLike like3 = createPostLike(3L, createPost(2L));
            when(postLikeRepository.findLikedPostsByUserIdWithCursor(eq(userId), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(like5, like3));

            MyPageLikedPostListResponse response = postQueryService.retrieveMyLikedPosts(userId, likedPostRequest(null, 2));

            assertThat(response.posts()).hasSize(2);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private void stubMyPosts(List<Post> posts) {
        lenient().when(postRepository.findPostsByUserId(any(), any(), any(), any(), any())).thenReturn(posts);
        lenient().when(postRepository.countTotalPostsByUserId(any())).thenReturn((long) posts.size());
    }

    private MyPagePostListRequest myPageRequest(Long cursor, int size, MyPagePostFilter filter) {
        return new MyPagePostListRequest(cursor, size, filter);
    }

    private MyPageLikedPostListRequest likedPostRequest(Long cursor, int size) {
        return new MyPageLikedPostListRequest(cursor, size);
    }

    private PostLike createPostLike(Long likeId, Post post) {
        PostLike postLike = PostLike.createPostLike(null, post);
        ReflectionTestUtils.setField(postLike, "id", likeId);
        return postLike;
    }

    private PostListRequest listRequest(int size, Long cursor) {
        return new PostListRequest(cursor, null, size, null, null, null, null, null, null, null, null, null, null, null, null, PostSortType.LATEST);
    }

    private PostListRequest listRequestWithSort(int size, Long cursor, Integer cursorValue, PostSortType sort) {
        return new PostListRequest(cursor, cursorValue, size, null, null, null, null, null, null, null, null, null, null, null, null, sort);
    }

    private Post createPost(Long postId) {
        return createPostWithEndDate(postId, TODAY.plusDays(5));
    }

    private Post createPostWithEndDate(Long postId, LocalDate endDate) {
        User user = User.builder()
                .email("user" + postId + "@a.com")
                .name("user" + postId)
                .oauthId("oauth-" + postId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", postId);

        Profile profile = new Profile(user);
        profile.setupInitialProfile(Gender.M, null, LocalDate.of(1995, 1, 1));
        profile.updateProfile("닉네임" + postId, null, ProfileImageType.DEFAULT, null, "소개");
        ReflectionTestUtils.setField(user, "profile", profile);

        Destination destination = Destination.builder()
                .countryCode("KR").countryName("대한민국").city("서울").build();

        LocalDate start = endDate.minusDays(2);
        LocalDate recruitDeadline = endDate.minusDays(1);
        Post post = Post.createPost(
                user, destination,
                "서울 여행 같이 가실 분 모집합니다",
                "함께 서울 여행할 동행자를 모집합니다. 편하게 신청해주세요.",
                start, endDate, 3, recruitDeadline,
                Gender.U, true, null, null, null, "[]", CompanionType.FULL
        );
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.of(2026, 1, 10, 0, 0));
        return post;
    }
}
