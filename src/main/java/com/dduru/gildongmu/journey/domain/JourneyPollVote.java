package com.dduru.gildongmu.journey.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journey_poll_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"option_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyPollVote extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private JourneyPollOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private JourneyPollVote(JourneyPollOption option, User user) {
        this.option = option;
        this.user = user;
    }

    public static JourneyPollVote create(JourneyPollOption option, User user) {
        return new JourneyPollVote(option, user);
    }
}
