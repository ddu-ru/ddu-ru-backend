package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.chat.domain.enums.ChatMessageType;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;
import com.dduru.gildongmu.report.dto.request.ReportCreateRequest;
import com.dduru.gildongmu.report.dto.response.ReportCreateResponse;
import com.dduru.gildongmu.report.exception.DuplicatePostReportException;
import com.dduru.gildongmu.report.exception.DuplicateReportException;
import com.dduru.gildongmu.report.exception.InvalidReportEvidenceException;
import com.dduru.gildongmu.report.exception.InvalidReportReasonException;
import com.dduru.gildongmu.report.exception.SelfPostReportNotAllowedException;
import com.dduru.gildongmu.report.exception.SelfReportNotAllowedException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 테스트")
class ReportServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ChatReportAccessValidator chatReportAccessValidator;

    @Mock
    private ReportEvidenceService reportEvidenceService;

    @InjectMocks
    private ReportService reportService;

    @Nested
    @DisplayName("신고 생성")
    class Create {

        @Test
        @DisplayName("성공 시 신고 ID와 상태를 반환한다")
        void success() {
            Long postId = 10L;
            Long userId = 1L;
            User reporter = createUser(userId, "reporter@dduru.com", "신고자");
            User postOwner = createUser(2L, "owner@dduru.com", "작성자");
            Post post = createPost(postId, postOwner);
            ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM_AD, "광고 게시물입니다.");

            when(userRepository.getByIdOrThrow(userId)).thenReturn(reporter);
            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
            when(reportRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
            when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> {
                Report report = invocation.getArgument(0);
                ReflectionTestUtils.setField(report, "id", 100L);
                return report;
            });

            ReportCreateResponse response = reportService.create(postId, userId, request);

            assertThat(response.reportId()).isEqualTo(100L);
            assertThat(response.status()).isNotNull();
            verify(reportRepository).saveAndFlush(any(Report.class));
        }

        @Test
        @DisplayName("본인 게시글 신고 시 예외가 발생한다")
        void selfPostThrowsException() {
            Long postId = 10L;
            Long userId = 1L;
            User user = createUser(userId, "same@dduru.com", "본인");
            Post post = createPost(postId, user);
            ReportCreateRequest request = new ReportCreateRequest(ReportReason.OTHER, "본인 글 신고 시도");

            when(userRepository.getByIdOrThrow(userId)).thenReturn(user);
            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);

            assertThatThrownBy(() -> reportService.create(postId, userId, request))
                    .isInstanceOf(SelfPostReportNotAllowedException.class);

            verify(reportRepository, never()).saveAndFlush(any(Report.class));
        }

        @Test
        @DisplayName("이미 신고한 게시글이면 예외가 발생한다")
        void duplicateReportThrowsException() {
            Long postId = 10L;
            Long userId = 1L;
            User reporter = createUser(userId, "reporter@dduru.com", "신고자");
            User postOwner = createUser(2L, "owner@dduru.com", "작성자");
            Post post = createPost(postId, postOwner);
            ReportCreateRequest request = new ReportCreateRequest(ReportReason.DUPLICATE, "이미 신고한 게시물");

            when(userRepository.getByIdOrThrow(userId)).thenReturn(reporter);
            when(postRepository.getActiveByIdOrThrow(postId)).thenReturn(post);
            when(reportRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

            assertThatThrownBy(() -> reportService.create(postId, userId, request))
                    .isInstanceOf(DuplicatePostReportException.class);

            verify(reportRepository, never()).saveAndFlush(any(Report.class));
        }
    }

    @Test
    @DisplayName("사용자 신고 대상과 증빙 키를 저장한다")
    void userReportStoresTargetAndEvidence() {
        User reporter = createUser(1L, "r@test.com", "reporter");
        User target = createUser(2L, "t@test.com", "target");
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.getWithProfileByIdOrThrow(2L)).thenReturn(target);
        when(reportRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        reportService.reportUser(2L, 1L, new ReportCreateRequest(ReportReason.OTHER, "details", "key"));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        Report report = captor.getValue();
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.USER);
        assertThat(report.getPost()).isNull();
        assertThat(report.getReportedUser()).isSameAs(target);
        assertThat(report.getTargetSnapshot()).isEqualTo("target");
        assertThat(report.getEvidenceKey()).isEqualTo("key");
        verify(reportEvidenceService).validateKey(1L, "key");
    }

    @Test
    @DisplayName("본인은 신고할 수 없다")
    void cannotReportYourself() {
        assertThatThrownBy(() -> reportService.reportUser(1L, 1L,
                new ReportCreateRequest(ReportReason.OTHER, null)))
                .isInstanceOf(SelfReportNotAllowedException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("미처리 상태의 동일 사용자 신고는 중복 접수할 수 없다")
    void rejectsOpenUserReport() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(createUser(1L, "r@test.com", "r")));
        when(userRepository.getWithProfileByIdOrThrow(2L)).thenReturn(createUser(2L, "t@test.com", "t"));
        when(reportRepository.existsByTargetTypeAndReportedUserIdAndUserIdAndStatusIn(
                eq(ReportTargetType.USER), eq(2L), eq(1L), any())).thenReturn(true);
        assertThatThrownBy(() -> reportService.reportUser(2L, 1L,
                new ReportCreateRequest(ReportReason.OTHER, null))).isInstanceOf(DuplicateReportException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("메시지 신고 시 원문과 작성자를 저장한다")
    void messageReportPreservesContentAndSender() {
        User sender = createUser(2L, "s@test.com", "sender");
        ChatMessage message = ChatMessage.create(mock(ChatRoom.class), sender, ChatMessageType.TEXT, "original");
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(createUser(1L, "r@test.com", "r")));
        when(chatReportAccessValidator.getReportableMessage(7L, 8L, 1L)).thenReturn(message);
        when(reportRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        reportService.reportMessage(7L, 8L, 1L, new ReportCreateRequest(ReportReason.INAPPROPRIATE, null));
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTargetType()).isEqualTo(ReportTargetType.CHAT_MESSAGE);
        assertThat(captor.getValue().getReportedUser()).isSameAs(sender);
        assertThat(captor.getValue().getTargetSnapshot()).isEqualTo("TEXT\noriginal");
    }

    @Test
    @DisplayName("동일 메시지는 중복 신고할 수 없다")
    void rejectsDuplicateMessageReport() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(createUser(1L, "r@test.com", "r")));
        when(chatReportAccessValidator.getReportableMessage(7L, 8L, 1L)).thenReturn(mock(ChatMessage.class));
        when(reportRepository.existsByMessageIdAndUserId(8L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> reportService.reportMessage(7L, 8L, 1L,
                new ReportCreateRequest(ReportReason.OTHER, null))).isInstanceOf(DuplicateReportException.class);
    }

    @Test
    @DisplayName("잘못된 증빙 키를 전달하면 신고를 저장하지 않는다")
    void invalidEvidenceDoesNotSaveReport() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(createUser(1L, "r@test.com", "r")));
        when(userRepository.getWithProfileByIdOrThrow(2L)).thenReturn(createUser(2L, "t@test.com", "t"));
        doThrow(new InvalidReportEvidenceException()).when(reportEvidenceService).validateKey(1L, "other-key");
        assertThatThrownBy(() -> reportService.reportUser(2L, 1L,
                new ReportCreateRequest(ReportReason.OTHER, null, "other-key")))
                .isInstanceOf(InvalidReportEvidenceException.class);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("사용자 신고에 게시글 전용 사유를 사용할 수 없다")
    void postOnlyReasonIsRejectedForNewTargets() {
        assertThatThrownBy(() -> reportService.reportUser(2L, 1L,
                new ReportCreateRequest(ReportReason.DUPLICATE, null))).isInstanceOf(InvalidReportReasonException.class);
    }

    private User createUser(Long id, String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .oauthId("kakao-" + id)
                .oauthType(OauthType.KAKAO)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post createPost(Long postId, User owner) {
        Post post = Post.createPost(
                owner,
                Destination.builder().countryCode("KR").countryName("대한민국").city("서울").build(),
                "신고 대상 게시글 제목",
                "신고 대상 게시글 본문은 충분히 긴 내용으로 작성합니다.",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                5,
                LocalDate.now().plusDays(1),
                Gender.M,
                false,
                20,
                30,
                null,
                "[]",
                CompanionType.FULL
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
