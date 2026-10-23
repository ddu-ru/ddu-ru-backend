package com.dduru.gildongmu.superhost.service;

import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.domain.enums.PostStatus;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.Profile;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.profile.domain.enums.ProfileImageType;
import com.dduru.gildongmu.superhost.domain.SuperHostExposure;
import com.dduru.gildongmu.superhost.domain.SuperHostTicket;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostExposureStatus;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketSource;
import com.dduru.gildongmu.superhost.domain.enums.SuperHostTicketStatus;
import com.dduru.gildongmu.superhost.dto.response.MySuperHostStatusResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostApplyResponse;
import com.dduru.gildongmu.superhost.dto.response.SuperHostPostListResponse;
import com.dduru.gildongmu.superhost.exception.SuperHostPostNotApplicableException;
import com.dduru.gildongmu.superhost.repository.SuperHostExposureRepository;
import com.dduru.gildongmu.superhost.repository.SuperHostTicketRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperHostService 테스트")
class SuperHostServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 5, 12, 0);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private SuperHostTicketRepository superHostTicketRepository;
    @Mock
    private SuperHostExposureRepository superHostExposureRepository;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private SuperHostService superHostService;

    @BeforeEach
    void setUpTimeProvider() {
        lenient().when(timeProvider.now()).thenReturn(NOW);
        lenient().when(timeProvider.today()).thenReturn(NOW.toLocalDate());
    }

    @Nested
    @DisplayName("온보딩 보상 티켓 지급")
    class GrantOnboardingRewardTicket {

        @Test
        @DisplayName("사용자당 온보딩 티켓 3장을 한 번만 지급한다")
        @SuppressWarnings("unchecked")
        void issuesThreeTicketsOnce() {
            Long userId = 1L;
            User user = createUser(userId);

            when(userRepository.findByIdWithLock(userId)).thenReturn(Optional.of(user));
            when(superHostTicketRepository.existsByUser_IdAndSource(
                    userId, SuperHostTicketSource.ONBOARDING_SURVEY)).thenReturn(false);

            superHostService.grantOnboardingRewardTicket(userId);

            ArgumentCaptor<List<SuperHostTicket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
            verify(superHostTicketRepository).saveAll(ticketCaptor.capture());
            assertThat(ticketCaptor.getValue())
                    .hasSize(3)
                    .allSatisfy(ticket -> {
                        assertThat(ticket.getSource()).isEqualTo(SuperHostTicketSource.ONBOARDING_SURVEY);
                        assertThat(ticket.getStatus()).isEqualTo(SuperHostTicketStatus.UNUSED);
                        assertThat(ticket.getBoostDurationDays()).isEqualTo(3);
                    });
        }

        @Test
        @DisplayName("온보딩 티켓이 이미 지급됐으면 추가 지급하지 않는다")
        void ignoresAlreadyGrantedReward() {
            Long userId = 1L;
            User user = createUser(userId);

            when(userRepository.findByIdWithLock(userId)).thenReturn(Optional.of(user));
            when(superHostTicketRepository.existsByUser_IdAndSource(
                    userId, SuperHostTicketSource.ONBOARDING_SURVEY)).thenReturn(true);

            superHostService.grantOnboardingRewardTicket(userId);

            verify(superHostTicketRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("티켓 적용")
    class ApplyTicketToPost {

        @Test
        @DisplayName("성공 시 노출이 ACTIVE로 생성된다")
        void success() {
            Long userId = 1L;
            Long postId = 10L;
            User user = createUser(userId);
            Post post = createPost(postId, user);
            SuperHostTicket ticket = SuperHostTicket.create(user, SuperHostTicketSource.ONBOARDING_SURVEY, 3);
            ReflectionTestUtils.setField(ticket, "id", 99L);

            when(postRepository.findSuperHostApplicableByIdAndUserIdWithLock(
                    postId, userId, PostStatus.OPEN, NOW.toLocalDate())).thenReturn(Optional.of(post));
            when(superHostExposureRepository.findLatestActiveEndedAtByPostId(
                    postId, SuperHostExposureStatus.ACTIVE, NOW)).thenReturn(Optional.empty());
            when(superHostTicketRepository.findFirstByUser_IdAndStatusOrderByIdAsc(userId, SuperHostTicketStatus.UNUSED))
                    .thenReturn(Optional.of(ticket));
            when(superHostTicketRepository.countByUser_IdAndStatus(userId, SuperHostTicketStatus.UNUSED)).thenReturn(0L);

            SuperHostApplyResponse response = superHostService.applyTicketToPost(userId, postId);

            ArgumentCaptor<SuperHostExposure> exposureCaptor = ArgumentCaptor.forClass(SuperHostExposure.class);
            verify(superHostExposureRepository).save(exposureCaptor.capture());
            assertThat(exposureCaptor.getValue().getStatus()).isEqualTo(SuperHostExposureStatus.ACTIVE);
            assertThat(ticket.getStatus()).isEqualTo(SuperHostTicketStatus.USED);
            assertThat(response.postId()).isEqualTo(postId);
            assertThat(response.ticketId()).isEqualTo(99L);
            assertThat(response.startedAt()).isEqualTo(NOW);
            assertThat(response.endsAt()).isEqualTo(NOW.plusDays(3));
            assertThat(response.unusedTicketCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("적용 중인 게시글에 티켓을 추가하면 최종 종료 시각부터 연장된다")
        void extendsFromCurrentFinalEndTime() {
            Long userId = 1L;
            Long postId = 10L;
            User user = createUser(userId);
            Post post = createPost(postId, user);
            SuperHostTicket ticket = SuperHostTicket.create(user, SuperHostTicketSource.ONBOARDING_SURVEY, 3);
            ReflectionTestUtils.setField(ticket, "id", 100L);
            LocalDateTime currentEndsAt = NOW.plusDays(2);

            when(postRepository.findSuperHostApplicableByIdAndUserIdWithLock(
                    postId, userId, PostStatus.OPEN, NOW.toLocalDate())).thenReturn(Optional.of(post));
            when(superHostTicketRepository.findFirstByUser_IdAndStatusOrderByIdAsc(userId, SuperHostTicketStatus.UNUSED))
                    .thenReturn(Optional.of(ticket));
            when(superHostExposureRepository.findLatestActiveEndedAtByPostId(
                    postId, SuperHostExposureStatus.ACTIVE, NOW)).thenReturn(Optional.of(currentEndsAt));
            when(superHostTicketRepository.countByUser_IdAndStatus(userId, SuperHostTicketStatus.UNUSED)).thenReturn(0L);

            SuperHostApplyResponse response = superHostService.applyTicketToPost(userId, postId);

            ArgumentCaptor<SuperHostExposure> exposureCaptor = ArgumentCaptor.forClass(SuperHostExposure.class);
            verify(superHostExposureRepository).save(exposureCaptor.capture());
            assertThat(exposureCaptor.getValue().getStartedAt()).isEqualTo(NOW);
            assertThat(exposureCaptor.getValue().getEndedAt()).isEqualTo(currentEndsAt.plusDays(3));
            assertThat(response.endsAt()).isEqualTo(currentEndsAt.plusDays(3));
        }

        @Test
        @DisplayName("적용 불가 게시글이면 예외가 발생한다")
        void failsWhenPostNotApplicable() {
            when(postRepository.findSuperHostApplicableByIdAndUserIdWithLock(
                    10L, 1L, PostStatus.OPEN, NOW.toLocalDate())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> superHostService.applyTicketToPost(1L, 10L))
                    .isInstanceOf(SuperHostPostNotApplicableException.class);
        }
    }

    @Nested
    @DisplayName("슈퍼호스트 조회")
    class Query {

        @Test
        @DisplayName("슈퍼호스트 목록은 ACTIVE 노출로 구성된다")
        void retrieveSuperHostPostsReturnsActiveList() {
            User user = createUser(1L);
            Post post = createPost(10L, user);
            SuperHostTicket ticket = SuperHostTicket.create(user, SuperHostTicketSource.ONBOARDING_SURVEY, 3);
            LocalDateTime endsAt = NOW.plusDays(3);
            SuperHostExposure exposure = SuperHostExposure.create(ticket, user, post, NOW, endsAt);

            when(superHostExposureRepository.findVisibleExposures(any(), any(), any(Pageable.class))).thenReturn(List.of(exposure));

            SuperHostPostListResponse response = superHostService.retrieveSuperHostPosts(5);

            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).id()).isEqualTo(10L);
            assertThat(response.items().get(0).author().isSuperHost()).isTrue();
        }

        @Test
        @DisplayName("만료 노출 종료 처리는 ACTIVE -> ENDED 갱신 수를 반환한다")
        void endExpiredExposuresReturnsUpdatedCount() {
            when(superHostExposureRepository.endExpiredExposures(
                    eq(SuperHostExposureStatus.ACTIVE),
                    eq(SuperHostExposureStatus.ENDED),
                    any(LocalDateTime.class)
            )).thenReturn(2);

            int result = superHostService.endExpiredExposures();

            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("내 슈퍼호스트 관리 조회 시 적용 중과 미적용 게시글을 분리한다")
        void getMyStatusSeparatesAppliedAndAvailablePosts() {
            Long userId = 1L;
            User user = createUser(userId);
            Post appliedPost = createPost(10L, user);
            Post availablePost = createPost(20L, user);
            LocalDateTime finalEndsAt = NOW.plusDays(5);

            when(superHostTicketRepository.countByUser_IdAndStatus(userId, SuperHostTicketStatus.UNUSED)).thenReturn(2L);
            when(postRepository.findSuperHostManageablePostsByUserId(
                    userId, PostStatus.OPEN, NOW.toLocalDate())).thenReturn(List.of(availablePost, appliedPost));
            when(superHostExposureRepository.findActivePostIdsWithEndedAt(
                    List.of(20L, 10L), SuperHostExposureStatus.ACTIVE, NOW))
                    .thenReturn(List.<Object[]>of(new Object[]{10L, finalEndsAt}));

            MySuperHostStatusResponse response = superHostService.getMyStatus(userId);

            assertThat(response.unusedTicketCount()).isEqualTo(2);
            assertThat(response.appliedPosts()).singleElement().satisfies(post -> {
                assertThat(post.postId()).isEqualTo(10L);
                assertThat(post.superHostEndsAt()).isEqualTo(finalEndsAt);
            });
            assertThat(response.availablePosts()).singleElement().satisfies(post -> {
                assertThat(post.postId()).isEqualTo(20L);
                assertThat(post.superHostEndsAt()).isNull();
            });
        }
    }

    private User createUser(Long id) {
        User user = User.builder()
                .email("user@test.com")
                .name("tester")
                .oauthId("oauth-id")
                .oauthType(OauthType.KAKAO)
                .build();
        Profile profile = new Profile(user);
        ReflectionTestUtils.setField(profile, "profileImageType", ProfileImageType.DEFAULT);
        ReflectionTestUtils.setField(profile, "nickname", "tester");
        ReflectionTestUtils.setField(profile, "gender", Gender.U);
        ReflectionTestUtils.setField(user, "profile", profile);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(Long postId, User owner) {
        Destination destination = Destination.builder()
                .countryCode("KR")
                .countryName("대한민국")
                .city("제주")
                .build();
        ReflectionTestUtils.setField(destination, "id", 1L);

        Post post = Post.createPost(
                owner,
                destination,
                "제목입니다",
                "내용은 스무 글자 이상으로 작성합니다.",
                NOW.toLocalDate().plusDays(3),
                NOW.toLocalDate().plusDays(5),
                4,
                NOW.toLocalDate().plusDays(2),
                Gender.U,
                true,
                null,
                null,
                null,
                "[]",
                CompanionType.FULL
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
