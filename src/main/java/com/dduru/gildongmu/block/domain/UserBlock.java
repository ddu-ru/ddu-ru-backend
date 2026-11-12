package com.dduru.gildongmu.block.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_blocks_blocker_blocked", columnNames = {"blocker_user_id", "blocked_user_id"}),
        indexes = @Index(name = "idx_user_blocks_blocker_id", columnList = "blocker_user_id, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_user_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    private UserBlock(User blocker, User blockedUser) {
        this.blocker = blocker;
        this.blockedUser = blockedUser;
    }

    public static UserBlock create(User blocker, User blockedUser) {
        return new UserBlock(blocker, blockedUser);
    }
}
