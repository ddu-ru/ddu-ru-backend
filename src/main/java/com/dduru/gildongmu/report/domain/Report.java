package com.dduru.gildongmu.report.domain;

import com.dduru.gildongmu.chat.domain.ChatMessage;
import com.dduru.gildongmu.chat.domain.ChatRoom;
import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.report.domain.enums.ReportReason;
import com.dduru.gildongmu.report.domain.enums.ReportStatus;
import com.dduru.gildongmu.report.domain.enums.ReportTargetType;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "user_id"}),
                @UniqueConstraint(name = "uk_reports_message_reporter", columnNames = {"message_id", "user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType = ReportTargetType.POST;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @Column(name = "target_snapshot", columnDefinition = "TEXT")
    private String targetSnapshot;

    @Column(name = "evidence_key", length = 512)
    private String evidenceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private ReportReason reason;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.RECEIVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Builder
    public Report(User user, Post post, ReportReason reason, String description) {
        this.user = user;
        this.post = post;
        this.reason = reason;
        this.description = description;
        this.targetSnapshot = post == null ? null : post.getTitle() + "\n" + post.getContent();
    }

    public static Report createReport(User user, Post post, ReportReason reason, String description) {
        return Report.builder()
                .user(user)
                .post(post)
                .reason(reason)
                .description(description)
                .build();
    }

    public static Report forUser(User reporter, User target, ReportReason reason, String description) {
        Report report = new Report(reporter, null, reason, description);
        report.targetType = ReportTargetType.USER;
        report.reportedUser = target;
        report.targetSnapshot = target.getProfile() == null ? target.getName() : target.getProfile().getNickname();
        return report;
    }

    public static Report forMessage(User reporter, ChatMessage message, ReportReason reason, String description) {
        Report report = new Report(reporter, null, reason, description);
        report.targetType = ReportTargetType.CHAT_MESSAGE;
        report.message = message;
        report.chatRoom = message.getRoom();
        report.reportedUser = message.getSender();
        report.targetSnapshot = message.getMessageType().name() + "\n" + message.getContent();
        return report;
    }

    public void attachEvidence(String evidenceKey) {
        this.evidenceKey = evidenceKey;
    }

    public void updateByAdmin(ReportStatus newStatus, User reviewer, String reviewNote, LocalDateTime now) {
        this.status = newStatus;
        this.reviewer = reviewer;
        this.reviewedAt = now;
        this.reviewNote = reviewNote;
    }
}
