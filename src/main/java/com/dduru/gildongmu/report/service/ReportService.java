package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.auth.exception.UserNotFoundException;
import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.repository.PostRepository;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;
import com.dduru.gildongmu.report.dto.request.ReportCreateRequest;
import com.dduru.gildongmu.report.dto.response.ReportCreateResponse;
import com.dduru.gildongmu.report.exception.DuplicatePostReportException;
import com.dduru.gildongmu.report.exception.DuplicateReportException;
import com.dduru.gildongmu.report.exception.InvalidReportReasonException;
import com.dduru.gildongmu.report.exception.SelfPostReportNotAllowedException;
import com.dduru.gildongmu.report.exception.SelfReportNotAllowedException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ReportService {

    private static final List<ReportStatus> OPEN_STATUSES = List.of(ReportStatus.RECEIVED, ReportStatus.IN_REVIEW);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ChatReportAccessValidator chatReportAccessValidator;
    private final ReportEvidenceService reportEvidenceService;

    public ReportCreateResponse create(Long postId, Long userId, ReportCreateRequest request) {
        User user = userRepository.getByIdOrThrow(userId);
        Post post = postRepository.getActiveByIdOrThrow(postId);
        validateReport(post, userId);

        Report report = Report.createReport(user, post, request.reason(), request.description());
        attachEvidence(report, userId, request);
        Report savedReport = saveReportOrThrowDuplicate(report, postId, userId);
        log.info("게시글 신고 생성 완료 - reportId: {}, postId: {}, userId: {}",
                savedReport.getId(), postId, userId);
        return ReportCreateResponse.from(savedReport);
    }

    public ReportCreateResponse reportUser(Long targetUserId, Long userId, ReportCreateRequest request) {
        validateNonPostReason(request.reason());
        if (targetUserId.equals(userId)) {
            throw new SelfReportNotAllowedException();
        }
        User reporter = lockReporter(userId);
        User target = userRepository.getWithProfileByIdOrThrow(targetUserId);
        if (reportRepository.existsByTargetTypeAndReportedUserIdAndUserIdAndStatusIn(
                ReportTargetType.USER, targetUserId, userId, OPEN_STATUSES)) {
            throw new DuplicateReportException();
        }
        return saveNewReport(Report.forUser(reporter, target, request.reason(), request.description()), request);
    }

    public ReportCreateResponse reportMessage(Long roomId, Long messageId, Long userId, ReportCreateRequest request) {
        validateNonPostReason(request.reason());
        User reporter = lockReporter(userId);
        ChatMessage message = chatReportAccessValidator.getReportableMessage(roomId, messageId, userId);
        if (reportRepository.existsByMessageIdAndUserId(messageId, userId)) {
            throw new DuplicateReportException();
        }
        return saveNewReport(Report.forMessage(reporter, message, request.reason(), request.description()), request);
    }

    private User lockReporter(Long userId) {
        // Serialize duplicate checks for reports that may be submitted again after review.
        return userRepository.findByIdWithLock(userId).orElseThrow(UserNotFoundException::new);
    }

    private ReportCreateResponse saveNewReport(Report report, ReportCreateRequest request) {
        attachEvidence(report, report.getUser().getId(), request);
        return ReportCreateResponse.from(reportRepository.saveAndFlush(report));
    }

    private void attachEvidence(Report report, Long userId, ReportCreateRequest request) {
        if (request.evidenceKey() != null) {
            reportEvidenceService.validateKey(userId, request.evidenceKey());
            report.attachEvidence(request.evidenceKey());
        }
    }

    private void validateNonPostReason(ReportReason reason) {
        if (reason == null || reason == ReportReason.DUPLICATE || reason == ReportReason.FALSE_INFO) {
            throw new InvalidReportReasonException();
        }
    }

    private Report saveReportOrThrowDuplicate(Report report, Long postId, Long userId) {
        try {
            return reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            if (!(e.getMostSpecificCause() instanceof SQLException sql)
                    || (sql.getErrorCode() != 1062 && !"23505".equals(sql.getSQLState()))) {
                throw e;
            }
            log.warn("중복 신고 동시성 충돌 - postId: {}, userId: {}", postId, userId);
            throw new DuplicatePostReportException();
        }
    }

    private void validateReport(Post post, Long userId) {
        if (post.getUser().getId().equals(userId)) {
            throw new SelfPostReportNotAllowedException();
        }

        if (reportRepository.existsByPostIdAndUserId(post.getId(), userId)) {
            throw new DuplicatePostReportException();
        }
    }
}
