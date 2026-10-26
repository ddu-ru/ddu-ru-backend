package com.dduru.gildongmu.journey.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.journey.domain.enums.JourneyPollStatus;
import com.dduru.gildongmu.journey.exception.InvalidJourneyPollException;
import com.dduru.gildongmu.journey.exception.JourneyPollClosedException;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "journey_polls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyPoll extends BaseTimeEntity {
    public static final int MAX_TEXT_LENGTH = 100;
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String title;

    @Column(nullable = false)
    private boolean allowMultipleChoices;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private boolean allowOptionAddition;

    @Column(nullable = false)
    private LocalDateTime closesAt;

    private JourneyPoll(Journey journey, User author, String title, boolean allowMultipleChoices,
                        boolean anonymous, boolean allowOptionAddition, LocalDateTime closesAt, LocalDateTime now) {
        this.journey = journey;
        this.author = author;
        this.title = normalizeText(title);
        if (closesAt == null || !closesAt.isAfter(now)) {
            throw InvalidJourneyPollException.invalidClosingTime();
        }
        this.allowMultipleChoices = allowMultipleChoices;
        this.anonymous = anonymous;
        this.allowOptionAddition = allowOptionAddition;
        this.closesAt = closesAt;
    }

    public static JourneyPoll create(Journey journey, User author, String title, boolean allowMultipleChoices,
                                     boolean anonymous, boolean allowOptionAddition, LocalDateTime closesAt,
                                     LocalDateTime now) {
        return new JourneyPoll(journey, author, title, allowMultipleChoices, anonymous, allowOptionAddition, closesAt, now);
    }

    public JourneyPollStatus statusAt(LocalDateTime now) {
        return now.isBefore(closesAt) ? JourneyPollStatus.OPEN : JourneyPollStatus.CLOSED;
    }

    public void validateOpen(LocalDateTime now) {
        if (statusAt(now) == JourneyPollStatus.CLOSED) {
            throw new JourneyPollClosedException();
        }
    }

    public void validateSelectionCount(int count) {
        if (count < 1 || count > MAX_OPTIONS || (!allowMultipleChoices && count != 1)) {
            throw InvalidJourneyPollException.invalidSelection();
        }
    }

    public static String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            throw InvalidJourneyPollException.invalidText();
        }
        String normalized = text.strip();
        if (normalized.codePointCount(0, normalized.length()) > MAX_TEXT_LENGTH) {
            throw InvalidJourneyPollException.invalidText();
        }
        return normalized;
    }
}
