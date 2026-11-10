package com.dduru.gildongmu.admin.report.service;

import com.dduru.gildongmu.admin.report.dto.request.AdminReportUpdateRequest;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportListResponse;
import com.dduru.gildongmu.admin.report.dto.response.AdminReportResponse;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.exception.ReportNotFoundException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.domain.enums.OauthType;
import com.dduru.gildongmu.user.domain.enums.Role;
import com.dduru.gildongmu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.context.ApplicationEventPublisher;
import com.dduru.gildongmu.report.event.ReportReviewedEvent;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService 테스트")
class AdminReportServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 11, 12, 0);

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminReportService adminReportService;

    @Nested
    @DisplayName("신고 목록 조회")
    class FindAll {

        @Test
        @DisplayName("상태 필터를 레포지토리에 전달한다")
        void passesFilterToRepository() {
            ReportStatus status = ReportStatus.IN_REVIEW;
            Pageable pageable = PageRequest.of(0, 20);
            Report report = createReport(1L);
            Page<Report> page = new PageImpl<>(List.of(report), pageable, 1);

            when(reportRepository.findAllByStatusOptional(status, pageable)).thenReturn(page);

            AdminReportListResponse response = adminReportService.findAll(status, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).status()).isEqualTo(ReportStatus.RECEIVED);
            verify(reportRepository).findAllByStatusOptional(status, pageable);
        }
    }

    @Nested
    @DisplayName("신고 처리")
    class Update {

        @Test
        @DisplayName("reviewer/reviewedAt/status/reviewNote가 갱신된다")
        void updatesReviewerReviewedAtStatusAndReviewNote() {
            Long reportId = 11L;
            Long reviewerId = 99L;
            Report report = createReport(reportId);
            User reviewer = createUser(reviewerId, "admin@dduru.com", "관리자", Role.ADMIN);
            AdminReportUpdateRequest request = new AdminReportUpdateRequest(ReportStatus.RESOLVED, "검토 및 조치 완료");

            when(reportRepository.findByIdWithLock(reportId)).thenReturn(Optional.of(report));
            when(userRepository.getByIdOrThrow(reviewerId)).thenReturn(reviewer);
            when(timeProvider.now()).thenReturn(NOW);

            AdminReportResponse response = adminReportService.update(reportId, reviewerId, request);

            assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(response.reviewerId()).isEqualTo(reviewerId);
            assertThat(response.reviewerName()).isEqualTo("관리자");
            assertThat(response.reviewedAt()).isEqualTo(NOW);
            assertThat(response.reviewNote()).isEqualTo("검토 및 조치 완료");
            verify(eventPublisher).publishEvent(new ReportReviewedEvent(reportId, 2L, ReportStatus.RESOLVED));
        }

        @Test
        @DisplayName("존재하지 않는 신고를 처리하면 예외가 발생한다")
        void whenReportNotFoundThrowsException() {
            Long reportId = 999L;
            Long reviewerId = 1L;
            AdminReportUpdateRequest request = new AdminReportUpdateRequest(ReportStatus.REJECTED, "근거 부족");

            when(reportRepository.findByIdWithLock(reportId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminReportService.update(reportId, reviewerId, request))
                    .isInstanceOf(ReportNotFoundException.class);
        }
    }

    @Test
    void repeatedTerminalStatusDoesNotNotifyAgain() {
        Report report = createReport(11L);
        User reviewer = createUser(99L, "a@test.com", "admin", Role.ADMIN);
        report.updateByAdmin(ReportStatus.RESOLVED, reviewer, "done", NOW);
        when(reportRepository.findByIdWithLock(11L)).thenReturn(Optional.of(report));
        when(userRepository.getByIdOrThrow(99L)).thenReturn(reviewer);
        when(timeProvider.now()).thenReturn(NOW);
        adminReportService.update(11L, 99L, new AdminReportUpdateRequest(ReportStatus.RESOLVED, "updated note"));
        verify(eventPublisher, never()).publishEvent(any(ReportReviewedEvent.class));
    }

    @Test
    void userReportWithoutPostIsIncludedInAdminList() {
        Report report = Report.forUser(createUser(1L, "r@test.com", "r", Role.USER),
                createUser(2L, "t@test.com", "t", Role.USER), ReportReason.OTHER, "details");
        Pageable pageable = PageRequest.of(0, 20);
        when(reportRepository.findAllByStatusOptional(null, pageable)).thenReturn(new PageImpl<>(List.of(report)));
        AdminReportResponse item = adminReportService.findAll(null, pageable).content().get(0);
        assertThat(item.targetType()).isEqualTo(ReportTargetType.USER);
        assertThat(item.reportedUserId()).isEqualTo(2L);
        assertThat(item.postId()).isNull();
        assertThat(item.postTitle()).isNull();
    }

    private Report createReport(Long reportId) {
        User reporter = createUser(2L, "reporter@dduru.com", "신고자", Role.USER);
        User owner = createUser(3L, "owner@dduru.com", "작성자", Role.USER);
        Post post = createPost(21L, owner);
        Report report = Report.createReport(reporter, post, ReportReason.SPAM_AD, "스팸 신고");
        ReflectionTestUtils.setField(report, "id", reportId);
        return report;
    }

    private Post createPost(Long postId, User owner) {
        Post post = Post.createPost(
                owner,
                Destination.builder().countryCode("KR").countryName("대한민국").city("서울").build(),
                "신고 대상 게시글",
                "신고 대상 게시글 본문은 충분한 길이로 작성합니다.",
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

    private User createUser(Long id, String email, String name, Role role) {
        User user = User.builder()
                .email(email)
                .name(name)
                .oauthId("kakao-" + id)
                .oauthType(OauthType.KAKAO)
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
