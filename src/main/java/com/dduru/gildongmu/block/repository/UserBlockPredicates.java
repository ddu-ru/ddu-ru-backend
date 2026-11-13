package com.dduru.gildongmu.block.repository;

import com.dduru.gildongmu.block.domain.QUserBlock;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;

public final class UserBlockPredicates {

    private UserBlockPredicates() {
    }

    public static BooleanExpression noBlockBetween(Long viewerId, NumberPath<Long> otherUserId) {
        if (viewerId == null) {
            return null;
        }

        QUserBlock userBlock = new QUserBlock("visibilityUserBlock");
        BooleanExpression blockedByViewer = userBlock.blocker.id.eq(viewerId)
                .and(userBlock.blockedUser.id.eq(otherUserId));
        BooleanExpression viewerIsBlocked = userBlock.blocker.id.eq(otherUserId)
                .and(userBlock.blockedUser.id.eq(viewerId));

        return JPAExpressions.selectOne()
                .from(userBlock)
                .where(blockedByViewer.or(viewerIsBlocked))
                .notExists();
    }
}
