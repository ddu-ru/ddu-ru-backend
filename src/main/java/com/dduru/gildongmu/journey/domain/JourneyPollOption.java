package com.dduru.gildongmu.journey.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journey_poll_options", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"poll_id", "display_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyPollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private JourneyPoll poll;

    @Column(nullable = false, length = JourneyPoll.MAX_TEXT_LENGTH)
    private String content;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private JourneyPollOption(JourneyPoll poll, String content, int displayOrder) {
        this.poll = poll;
        this.content = JourneyPoll.normalizeText(content);
        this.displayOrder = displayOrder;
    }

    public static JourneyPollOption create(JourneyPoll poll, String content, int displayOrder) {
        return new JourneyPollOption(poll, content, displayOrder);
    }
}
