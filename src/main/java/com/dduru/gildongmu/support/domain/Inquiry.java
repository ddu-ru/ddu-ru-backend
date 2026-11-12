package com.dduru.gildongmu.support.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.support.domain.enums.InquiryCategory;
import com.dduru.gildongmu.support.domain.enums.InquiryStatus;
import com.dduru.gildongmu.support.exception.*;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;

@Entity
@Table(name = "inquiries", indexes = {
        @Index(name = "idx_inquiries_user_id", columnList = "user_id, id"),
        @Index(name = "idx_inquiries_status_id", columnList = "status, id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<InquiryImage> images = new ArrayList<>();

    @Column(name = "answer_content", length = 5000)
    private String answerContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    private LocalDateTime answeredAt;

    private LocalDateTime answerModifiedAt;

    public static Inquiry create(
            User user,
            InquiryCategory category,
            String content,
            List<String> imageUrls
    ) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.status = InquiryStatus.WAITING;
        inquiry.replaceContent(category, content, imageUrls);
        return inquiry;
    }

    public void requireOwner(Long userId) {
        if (!user.getId().equals(userId)) throw new InquiryAccessDeniedException();
    }

    public void update(InquiryCategory category, String content, List<String> imageUrls) {
        if (!isEditable()) throw new InquiryAlreadyAnsweredException();
        replaceContent(category, content, imageUrls);
    }

    public boolean isEditable() {
        return status == InquiryStatus.WAITING;
    }

    public static void validate(InquiryCategory category, String content, List<String> imageUrls) {
        if (category == null || content == null || content.isBlank() || content.length() > 500
                || imageUrls == null || imageUrls.size() > 4
                || imageUrls.stream().anyMatch(url -> url == null || url.isBlank() || url.length() > 2048)) {
            throw new InvalidInquiryException();
        }
    }

    private void replaceContent(InquiryCategory category, String content, List<String> imageUrls) {
        validate(category, content, imageUrls);
        this.category = category;
        this.content = content;
        images.clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(InquiryImage.of(this, imageUrls.get(i), i));
        }
    }

    public void answer(String content, User admin, LocalDateTime now) {
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw new InvalidInquiryAnswerException();
        }
        if (answeredAt == null) answeredAt = now;
        answerContent = content;
        answeredBy = admin;
        answerModifiedAt = now;
        status = InquiryStatus.ANSWERED;
    }
}
