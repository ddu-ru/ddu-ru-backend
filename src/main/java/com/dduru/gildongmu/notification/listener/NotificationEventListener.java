package com.dduru.gildongmu.notification.listener;

import com.dduru.gildongmu.chat.event.PostUpdatedEvent;
import com.dduru.gildongmu.fcm.service.FcmPushService;
import com.dduru.gildongmu.journey.event.JourneyNoticeCreatedEvent;
import com.dduru.gildongmu.journey.event.ScheduleCanceledEvent;
import com.dduru.gildongmu.journey.event.ScheduleCreatedEvent;
import com.dduru.gildongmu.journey.event.ScheduleUpdatedEvent;
import com.dduru.gildongmu.journey.repository.JourneyMemberRepository;
import com.dduru.gildongmu.like.repository.PostLikeRepository;
import com.dduru.gildongmu.notification.domain.Notification;
import com.dduru.gildongmu.notification.domain.enums.NotificationType;
import com.dduru.gildongmu.notification.domain.enums.ResourceType;
import com.dduru.gildongmu.notification.service.NotificationPersistService;
import com.dduru.gildongmu.participation.event.MatchAppliedEvent;
import com.dduru.gildongmu.participation.event.MatchApprovedEvent;
import com.dduru.gildongmu.user.repository.UserRepository;
import com.dduru.gildongmu.report.event.ReportReviewedEvent;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

// 모든 핸들러는 AFTER_COMMIT — 원래 트랜잭션 롤백 시 알림이 발송되는 것을 방지
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationPersistService notificationPersistService;
    private final JourneyMemberRepository journeyMemberRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;
    private final PostLikeRepository postLikeRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportReviewed(ReportReviewedEvent event) {
        try {
            String body = event.status() == ReportStatus.RESOLVED
                    ? "접수하신 신고의 검토 및 처리가 완료되었습니다."
                    : "접수하신 신고를 검토했으나 조치 대상으로 확인되지 않았습니다.";
            notifyUsers(List.of(event.reporterId()), NotificationType.REPORT_REVIEWED, body,
                    ResourceType.REPORT, event.reportId(), NotificationType.REPORT_REVIEWED.getTitle());
        } catch (Exception e) {
            log.error("REPORT_REVIEWED 알림 저장 실패 - reportId={}", event.reportId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchApplied(MatchAppliedEvent event) {
        try {
            String body = event.actorNickname() + " 님이 매칭을 신청했습니다.";
            notificationPersistService.save(Notification.create(
                    userRepository.getReferenceById(event.recipientUserId()),
                    NotificationType.MATCH_APPLIED, body, ResourceType.MATCH, event.participationId()
            ));
            if (userRepository.existsByIdAndNotificationEnabled(event.recipientUserId(), true)) {
                fcmPushService.sendToUser(event.recipientUserId(), "매칭 신청 도착", body,
                        FcmPushService.dataPayload(ResourceType.MATCH, event.participationId()));
            }
        } catch (Exception e) {
            log.error("MATCH_APPLIED 알림 저장 실패 - participationId={}", event.participationId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchApproved(MatchApprovedEvent event) {
        try {
            String body = event.approverNickname() + " 님과 매칭이 성사되었습니다.";
            notificationPersistService.save(Notification.create(
                    userRepository.getReferenceById(event.applicantUserId()),
                    NotificationType.MATCH_APPROVED, body, ResourceType.JOURNEY, event.journeyId()
            ));
            if (userRepository.existsByIdAndNotificationEnabled(event.applicantUserId(), true)) {
                fcmPushService.sendToUser(event.applicantUserId(), "매칭 승인", body,
                        FcmPushService.dataPayload(ResourceType.JOURNEY, event.journeyId()));
            }
        } catch (Exception e) {
            log.error("MATCH_APPROVED 알림 저장 실패 - journeyId={}", event.journeyId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleJourneyNoticeCreated(JourneyNoticeCreatedEvent event) {
        try {
            notifyJourneyMembers(
                    event.journeyId(), event.actorUserId(),
                    NotificationType.JOURNEY_NOTICE,
                    event.journeyTitle() + " 에 공지가 등록되었습니다.",
                    "공지 등록", ResourceType.JOURNEY_POST, event.journeyPostId()
            );
        } catch (Exception e) {
            log.error("JOURNEY_NOTICE 알림 저장 실패 - journeyPostId={}", event.journeyPostId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleCreated(ScheduleCreatedEvent event) {
        try {
            notifyJourneyMembers(
                    event.journeyId(), event.actorUserId(),
                    NotificationType.SCHEDULE_CREATED,
                    event.scheduleTitle() + " 일정이 추가되었습니다.",
                    "일정 생성", ResourceType.SCHEDULE, event.scheduleId()
            );
        } catch (Exception e) {
            log.error("SCHEDULE_CREATED 알림 저장 실패 - scheduleId={}", event.scheduleId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleUpdated(ScheduleUpdatedEvent event) {
        try {
            notifyJourneyMembers(
                    event.journeyId(), event.actorUserId(),
                    NotificationType.SCHEDULE_UPDATED,
                    event.scheduleTitle() + " 일정이 변경되었습니다.",
                    "일정 변경", ResourceType.SCHEDULE, event.scheduleId()
            );
        } catch (Exception e) {
            log.error("SCHEDULE_UPDATED 알림 저장 실패 - scheduleId={}", event.scheduleId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleScheduleCanceled(ScheduleCanceledEvent event) {
        try {
            notifyJourneyMembers(
                    event.journeyId(), event.actorUserId(),
                    NotificationType.SCHEDULE_CANCELED,
                    event.scheduleTitle() + " 일정이 취소되었습니다.",
                    "일정 취소", ResourceType.SCHEDULE, event.scheduleId()
            );
        } catch (Exception e) {
            log.error("SCHEDULE_CANCELED 알림 저장 실패 - scheduleId={}", event.scheduleId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostUpdated(PostUpdatedEvent event) {
        try {
            List<Long> recipientIds = postLikeRepository.findUserIdsByPostId(event.postId());
            String body = "관심 있는 모집글에 변경이 있습니다.";
            notifyUsers(recipientIds, NotificationType.POST_UPDATED, body,
                    ResourceType.POST, event.postId(), "찜한 글 업데이트");
        } catch (Exception e) {
            log.error("POST_UPDATED 알림 저장 실패 - postId={}", event.postId(), e);
        }
    }

    private void notifyJourneyMembers(
            Long journeyId, Long actorUserId,
            NotificationType type, String body, String pushTitle, ResourceType resourceType, Long resourceId
    ) {
        List<Long> recipientIds = journeyMemberRepository
                .findMemberIdsExcludingActor(journeyId, actorUserId);
        notifyUsers(recipientIds, type, body, resourceType, resourceId, pushTitle);
    }

    private void notifyUsers(
            List<Long> recipientIds, NotificationType type,
            String body, ResourceType resourceType, Long resourceId, String pushTitle
    ) {
        if (recipientIds.isEmpty()) return;
        List<Notification> notifications = recipientIds.stream()
                .map(id -> Notification.create(
                        userRepository.getReferenceById(id),
                        type, body, resourceType, resourceId
                ))
                .toList();
        notificationPersistService.saveAll(notifications);
        List<Long> fcmTargetIds = userRepository.findEnabledUserIds(recipientIds);
        if (fcmTargetIds.isEmpty()) return;
        fcmPushService.sendToUsers(fcmTargetIds, pushTitle, body, null,
                FcmPushService.dataPayload(resourceType, resourceId));
    }
}
