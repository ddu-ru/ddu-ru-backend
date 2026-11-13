package com.dduru.gildongmu.post.service;

import com.dduru.gildongmu.block.service.UserBlockService;
import com.dduru.gildongmu.chat.event.PostUpdatedEvent;
import com.dduru.gildongmu.chat.service.GroupChatRoomService;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.destination.repository.DestinationRepository;
import com.dduru.gildongmu.journey.domain.Journey;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.journey.repository.JourneyRepository;
import com.dduru.gildongmu.like.repository.PostLikeRepository;
import com.dduru.gildongmu.participation.service.ParticipationApplicantService;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.dto.request.PostCreateRequest;
import com.dduru.gildongmu.post.dto.request.PostStatusUpdateRequest;
import com.dduru.gildongmu.post.dto.request.PostUpdateRequest;
import com.dduru.gildongmu.post.dto.response.MyParticipationStatus;
import com.dduru.gildongmu.post.dto.response.PostCreateResponse;
import com.dduru.gildongmu.post.dto.response.PostDetailResponse;
import com.dduru.gildongmu.post.exception.InvalidPostContentException;
import com.dduru.gildongmu.post.exception.InvalidPostDateException;
import com.dduru.gildongmu.post.exception.InvalidPostTitleException;
import com.dduru.gildongmu.post.exception.InvalidPreferredAgeException;
import com.dduru.gildongmu.post.exception.PostAccessDeniedException;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.profile.utils.ProfileImageResolver;
import com.dduru.gildongmu.superhost.service.SuperHostService;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 5);
    private static final LocalDateTime NOW = TODAY.atTime(12, 0);

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private DestinationRepository destinationRepository;
    @Spy  private JsonConverter jsonConverter = new JsonConverter(new ObjectMapper());
    @Mock private GroupChatRoomService groupChatRoomService;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private ProfileImageResolver profileImageResolver;
    @Mock private ParticipationApplicantService participationApplicantService;
    @Mock private SuperHostService superHostService;
    @Mock private JourneyRepository journeyRepository;
    @Mock private JourneyMemberRepository journeyMemberRepository;
    @Mock private TimeProvider timeProvider;
    @Mock private S3ImageUrlValidator s3ImageUrlValidator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserBlockService userBlockService;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUpTimeProvider() {
        lenient().when(timeProvider.today()).thenReturn(TODAY);
        lenient().when(timeProvider.now()).thenReturn(NOW);
        lenient().when(s3ImageUrlValidator.validateAndNormalize(anyString(), any(S3ImageDirectory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("차단 관계에서는 게시글 상세 조회 전에 접근을 거절하고 조회수를 올리지 않는다")
    void rejectsBlockedPostDetailBeforeIncrementingViewCount() {
        Long postId = 10L;
        Long viewerId = 1L;
        User owner = createUser(2L, "owner");
        Post post = createBasicPost(postId, owner);
        when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
        doThrow(new com.dduru.gildongmu.block.exception.BlockedUserException())
                .when(userBlockService).validateNotBlocked(viewerId, owner.getId());

        assertThatThrownBy(() -> postService.recordViewAndGetDetail(postId, viewerId))
                .isInstanceOf(com.dduru.gildongmu.block.exception.BlockedUserException.class);

        verify(postRepository, never()).incrementViewCount(postId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 게시글 생성
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 생성")
    class Create {

        @Test
        @DisplayName("유효한 요청이면 생성 후 ID를 반환하고 모집 마감일은 여행 종료일 전날이다")
        void success() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            LocalDate startDate = TODAY.plusDays(10);
            LocalDate endDate = TODAY.plusDays(12);
            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    startDate, endDate, 5, Gender.M, false, 25, 35, null,
                    List.of("태그1"), CompanionType.FULL
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 100L);
                return post;
            });
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            PostCreateResponse response = postService.create(userId, request);

            assertThat(response.id()).isEqualTo(100L);

            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            Post savedPost = postCaptor.getValue();

            assertThat(savedPost.getCompanionType()).isEqualTo(CompanionType.FULL);
            assertThat(savedPost.getRecruitDeadline()).isEqualTo(endDate.minusDays(1));
            assertThat(savedPost.isAgeAny()).isFalse();
            assertThat(savedPost.getMinAge()).isEqualTo(25);
            assertThat(savedPost.getMaxAge()).isEqualTo(35);
            verify(journeyRepository).save(any());
            verify(journeyMemberRepository).save(any());
            verify(groupChatRoomService).createRoomForJourney(any(Journey.class), eq(user));
        }

        @Test
        @DisplayName("UNSPECIFIED 동행 방식을 보내면 UNSPECIFIED로 저장한다")
        void createsPostWithUnspecifiedCompanionType() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null,
                    List.of("태그1"), CompanionType.UNSPECIFIED
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 100L);
                return post;
            });
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.create(userId, request);

            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().getCompanionType()).isEqualTo(CompanionType.UNSPECIFIED);
        }

        @Test
        @DisplayName("journey는 생성 시 제목과 대표 사진을 post 초기값으로 저장한다")
        void createsJourneyWithCopiedTitleAndPhoto() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null,
                    "https://example.com/journey-cover.png",
                    List.of(), CompanionType.FULL
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 100L);
                return post;
            });
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.create(userId, request);

            ArgumentCaptor<Journey> journeyCaptor = ArgumentCaptor.forClass(Journey.class);
            verify(journeyRepository).save(journeyCaptor.capture());
            Journey savedJourney = journeyCaptor.getValue();

            assertThat(savedJourney.getTitle()).isEqualTo(request.title());
            assertThat(savedJourney.getPhotoUrl()).isEqualTo(request.photoUrl());
        }

        @Test
        @DisplayName("태그 앞뒤 공백은 DTO에서 제거되어 JSON으로 변환된다")
        @SuppressWarnings("unchecked")
        void tagsStrippedByDtoThenConvertedToJson() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, false, 25, 35, null,
                    List.of("  제주  ", "부산"), CompanionType.FULL
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 1L);
                return post;
            });
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.create(userId, request);

            ArgumentCaptor<List<String>> tagCaptor = ArgumentCaptor.forClass(List.class);
            verify(jsonConverter).convertListToJson(tagCaptor.capture());
            assertThat(tagCaptor.getValue()).containsExactly("제주", "부산");
        }

        @Test
        @DisplayName("제목의 앞뒤 공백은 자동으로 제거된다")
        void titleTrimmedBeforeSave() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "  제주 여행 모집합니다  ",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.create(userId, request);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("제주 여행 모집합니다");
        }

        @Test
        @DisplayName("제목이 5자 미만이면 InvalidPostTitleException이 발생한다")
        void titleTooShortThrows() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "짧음",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPostTitleException.class);
        }

        @Test
        @DisplayName("제목이 40자를 초과하면 InvalidPostTitleException이 발생한다")
        void titleTooLongThrows() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제".repeat(41),
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPostTitleException.class);
        }

        @Test
        @DisplayName("내용이 20자 미만이면 InvalidPostContentException이 발생한다")
        void contentTooShortThrows() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제주 여행 모집합니다",
                    "내용이 짧습니다",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPostContentException.class);
        }

        @Test
        @DisplayName("내용이 1000자를 초과하면 InvalidPostContentException이 발생한다")
        void contentTooLongThrows() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제주 여행 모집합니다",
                    "내".repeat(1001),
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPostContentException.class);
        }

        @Test
        @DisplayName("연령 무관이면 isAgeAny가 true이고 min/max는 null이다")
        void ageAnySavesFlagAndNullMinMax() {
            Long userId = 1L;
            Long destinationId = 10L;
            User user = createUser(userId, "user");
            Destination destination = createDestination("제주도", null);
            ReflectionTestUtils.setField(destination, "id", destinationId);

            PostCreateRequest request = new PostCreateRequest(
                    destinationId, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, null, null, null, List.of(), CompanionType.FULL
            );

            givenCreateContext(userId, destinationId, user, destination);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
            when(journeyRepository.save(any(Journey.class))).thenAnswer(inv -> inv.getArgument(0));

            postService.create(userId, request);

            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            Post saved = postCaptor.getValue();

            assertThat(saved.isAgeAny()).isTrue();
            assertThat(saved.getMinAge()).isNull();
            assertThat(saved.getMaxAge()).isNull();
        }

        @Test
        @DisplayName("연령 무관인데 min/max를 넣으면 예외가 발생한다")
        void ageAnyWithMinMaxThrows() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, true, 20, 30, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPreferredAgeException.class);
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 예외가 발생한다")
        void endBeforeStartThrowsInvalidPostDateException() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(5), TODAY.plusDays(3),
                    5, Gender.M, true, null, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPostDateException.class)
                    .hasMessageContaining("종료일");
        }

        @Test
        @DisplayName("구간을 쓰는데 min만 있으면 예외가 발생한다")
        void onlyMinAgeThrowsInvalidPreferredAgeException() {
            PostCreateRequest request = new PostCreateRequest(
                    10L, "제주 여행 같이 가실 분 모집합니다",
                    "내용은 스무글자 이상이어야 합니다!!!!!!!!!!!!!",
                    TODAY.plusDays(10), TODAY.plusDays(12),
                    5, Gender.M, false, 25, null, null, null, CompanionType.FULL
            );

            assertThatThrownBy(() -> postService.create(1L, request))
                    .isInstanceOf(InvalidPreferredAgeException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 게시글 수정
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 수정")
    class Update {

        @Test
        @DisplayName("작성자가 아니면 예외가 발생한다")
        void notOwnerThrowsPostAccessDeniedException() {
            Long postId = 1L;
            Long ownerId = 10L;
            Long requesterId = 99L;
            User owner = createUser(ownerId, "owner");
            Post post = createBasicPost(postId, owner);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            PostUpdateRequest updateRequest = new PostUpdateRequest(
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> postService.update(postId, requesterId, updateRequest))
                    .isInstanceOf(PostAccessDeniedException.class);
        }

        @Test
        @DisplayName("게시글 수정에 성공하면 채팅방 목록 메타데이터 갱신 이벤트를 발행한다")
        void updateSuccessPublishesPostUpdatedEvent() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            PostUpdateRequest request = new PostUpdateRequest(
                    null, "수정된 제목입니다", null, null, null, null, null, null, null, null, null, null, null
            );

            postService.update(postId, userId, request);

            assertThat(post.getTitle()).isEqualTo("수정된 제목입니다");
            verify(eventPublisher).publishEvent(new PostUpdatedEvent(postId));
        }

        @Test
        @DisplayName("제목을 4자 이하로 수정하면 InvalidPostTitleException이 발생한다")
        void updateWithShortTitleThrows() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            PostUpdateRequest request = new PostUpdateRequest(
                    null, "짧음", null, null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> postService.update(postId, userId, request))
                    .isInstanceOf(InvalidPostTitleException.class);
        }

        @Test
        @DisplayName("내용을 19자 이하로 수정하면 InvalidPostContentException이 발생한다")
        void updateWithShortContentThrows() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            PostUpdateRequest request = new PostUpdateRequest(
                    null, null, "내용이 짧습니다", null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> postService.update(postId, userId, request))
                    .isInstanceOf(InvalidPostContentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 게시글 상세 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 상세 조회")
    class RecordViewAndGetDetail {

        @Test
        @DisplayName("모집글 수정 권한과 표시 텍스트를 함께 응답한다")
        void returnsDetailWithEditCapabilityAndDisplayText() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Profile profile = attachProfile(owner, "호스트닉");
            Destination destination = createDestination("제주", "https://example.com/destination.png");

            Post post = createPost(
                    postId, owner, destination,
                    "제주 여행 같이 가실 분 모집합니다",
                    "함께 떠나는 여행입니다. 충분히 긴 설명을 넣어둡니다.",
                    TODAY.plusDays(2), TODAY.plusDays(4), 4, TODAY.plusDays(3),
                    Gender.U, true, null, null,
                    "https://example.com/photo.png", "[\"맛집\",\"바다\"]"
            );

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
            when(participationApplicantService.getParticipantsForPostDetail(eq(post), any(LocalDate.class))).thenReturn(List.of());
            when(participationApplicantService.getMyParticipationStatus(postId, userId, true))
                    .thenReturn(MyParticipationStatus.NONE);
            when(profileImageResolver.resolve(profile)).thenReturn("https://example.com/profile.png");

            PostDetailResponse response = postService.recordViewAndGetDetail(postId, userId);

            verify(postRepository).incrementViewCount(postId);
            assertThat(response.isOwner()).isTrue();
            assertThat(response.canEditPost()).isTrue();
            assertThat(response.tripDurationText()).isEqualTo("2박 3일");
            assertThat(response.recruitDeadlineDDay()).startsWith("D-");
            assertThat(response.tags()).containsExactly("맛집", "바다");
        }

        @Test
        @DisplayName("여행 시작 다음 날부터는 모집글 수정 권한이 없다")
        void afterTravelStartReturnsCannotEditPost() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Profile profile = attachProfile(owner, "호스트닉");
            Destination destination = createDestination("서울", "https://example.com/destination.png");

            Post post = createPost(
                    postId, owner, destination,
                    "서울 여행 같이 가실 분 모집합니다",
                    "이미 여행이 시작된 뒤의 충분히 긴 설명입니다.",
                    TODAY.minusDays(1), TODAY.plusDays(1), 4, TODAY.plusDays(1),
                    Gender.U, true, null, null,
                    "https://example.com/photo.png", "[\"야경\"]"
            );

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
            when(participationApplicantService.getParticipantsForPostDetail(eq(post), any(LocalDate.class))).thenReturn(List.of());
            when(participationApplicantService.getMyParticipationStatus(postId, userId, true))
                    .thenReturn(MyParticipationStatus.NONE);
            when(profileImageResolver.resolve(profile)).thenReturn("https://example.com/profile.png");

            PostDetailResponse response = postService.recordViewAndGetDetail(postId, userId);

            assertThat(response.isOwner()).isTrue();
            assertThat(response.canEditPost()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 자동 마감
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("자동 마감")
    class CloseExpiredPosts {

        @Test
        @DisplayName("자동 마감 시 슈퍼호스트 활성 노출도 함께 취소한다")
        void cancelsClosedPostExposure() {
            when(postRepository.closeExpiredPostsByDate(any(LocalDate.class))).thenReturn(2);

            int result = postService.closeExpiredPosts();

            assertThat(result).isEqualTo(2);
            verify(superHostService, times(1)).cancelActiveExposureByClosedPosts();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 게시글 삭제
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 삭제")
    class Delete {

        @Test
        @DisplayName("삭제 시 슈퍼호스트 활성 노출을 취소한다")
        void cancelsSuperHostExposureForPost() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            postService.delete(postId, userId);

            verify(superHostService).cancelActiveExposureByPostId(eq(postId));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 모집 상태 변경
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("게시글 모집 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("CLOSED로 변경하면 슈퍼호스트 활성 노출을 취소한다")
        void toClosedCancelsSuperHostExposure() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);
            ReflectionTestUtils.setField(post, "status", PostStatus.OPEN);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            postService.changeStatus(postId, userId, new PostStatusUpdateRequest(false));

            verify(superHostService).cancelActiveExposureByPostId(eq(postId));
        }

        @Test
        @DisplayName("OPEN으로 변경하면 슈퍼호스트 노출 취소를 호출하지 않는다")
        void toOpenDoesNotCancelSuperHostExposure() {
            Long postId = 1L;
            Long userId = 10L;
            User owner = createUser(userId, "owner");
            Post post = createBasicPost(postId, owner);
            ReflectionTestUtils.setField(post, "status", PostStatus.CLOSED);

            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            postService.changeStatus(postId, userId, new PostStatusUpdateRequest(true));

            verify(superHostService, never()).cancelActiveExposureByPostId(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private void givenCreateContext(Long userId, Long destinationId, User user, Destination destination) {
        when(userRepository.getByIdOrThrow(userId)).thenReturn(user);
        when(destinationRepository.getByIdOrThrow(destinationId)).thenReturn(destination);
    }

    private User createUser(Long userId, String name) {
        User user = User.builder()
                .email(name + "@a.com")
                .name(name)
                .oauthId("kakao-" + userId)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Profile attachProfile(User user, String nickname) {
        Profile profile = new Profile(user);
        profile.setupInitialProfile(Gender.F, null, LocalDate.of(1998, 1, 1));
        profile.updateProfile(nickname, null, ProfileImageType.DEFAULT, null, "소개");
        ReflectionTestUtils.setField(user, "profile", profile);
        return profile;
    }

    private Destination createDestination(String city, String imageUrl) {
        return Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city(city)
                .image(imageUrl)
                .build();
    }

    private Post createBasicPost(Long postId, User owner) {
        return createPost(
                postId, owner,
                createDestination("서울", null),
                "서울 여행 같이 가실 분 모집합니다",
                "함께 서울 여행할 동행자를 모집합니다. 편하게 신청해주세요.",
                TODAY.plusDays(1), TODAY.plusDays(3), 5, TODAY.plusDays(2),
                Gender.M, false, 20, 30, null, "[]"
        );
    }

    private Post createPost(
            Long postId, User owner, Destination destination,
            String title, String content,
            LocalDate startDate, LocalDate endDate,
            Integer recruitCapacity, LocalDate recruitDeadline,
            Gender preferredGender, boolean isAgeAny,
            Integer minAge, Integer maxAge,
            String photoUrl, String tags
    ) {
        Post post = Post.createPost(
                owner, destination, title, content,
                startDate, endDate, recruitCapacity, recruitDeadline,
                preferredGender, isAgeAny, minAge, maxAge,
                photoUrl, tags, CompanionType.FULL
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
