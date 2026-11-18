package com.dduru.gildongmu.post.share.dto;

import com.dduru.gildongmu.post.domain.Post;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record PostShareView(
        Long postId,
        String title,
        String photoUrl,
        String destination,
        String travelPeriod,
        int recruitCount,
        int recruitCapacity,
        String recruitmentStatus,
        String description
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d.");

    public static PostShareView from(Post post, LocalDate today) {
        String travelPeriod = "%s - %s".formatted(
                post.getStartDate().format(DATE_FORMATTER),
                post.getEndDate().format(DATE_FORMATTER)
        );
        String recruitmentStatus = isRecruitmentClosed(post, today) ? "모집 마감" : "모집 중";
        String description = "%s · %s · %d/%d명 · %s".formatted(
                post.getDestination().getCity(),
                travelPeriod,
                post.getRecruitCount(),
                post.getRecruitCapacity(),
                recruitmentStatus
        );

        return new PostShareView(
                post.getId(),
                post.getTitle(),
                post.getPhotoUrl(),
                post.getDestination().getCity(),
                travelPeriod,
                post.getRecruitCount(),
                post.getRecruitCapacity(),
                recruitmentStatus,
                description
        );
    }

    private static boolean isRecruitmentClosed(Post post, LocalDate today) {
        return post.isClosed()
                || post.isFull()
                || post.hasRecruitDeadlinePassed(today)
                || post.hasTravelStarted(today);
    }
}
