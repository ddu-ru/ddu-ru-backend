package com.dduru.gildongmu.post.share.dto;

import com.dduru.gildongmu.post.domain.Post;
import com.dduru.gildongmu.post.domain.enums.CompanionType;
import com.dduru.gildongmu.profile.domain.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record PostShareView(
        Long postId,
        String title,
        String content,
        String photoUrl,
        String destination,
        String travelPeriod,
        String tripDuration,
        int recruitCount,
        int recruitCapacity,
        String recruitmentStatus,
        String companionType,
        String ageCondition,
        String createdDate,
        int viewCount,
        List<String> tags,
        String description
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d.");
    private static final DateTimeFormatter CREATED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static PostShareView from(Post post, LocalDate today, List<String> tags) {
        String travelPeriod = "%s - %s".formatted(
                post.getStartDate().format(DATE_FORMATTER),
                post.getEndDate().format(DATE_FORMATTER)
        );
        String tripDuration = tripDuration(post.getStartDate(), post.getEndDate());
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
                post.getContent(),
                post.getPhotoUrl(),
                post.getDestination().getCity(),
                travelPeriod,
                tripDuration,
                post.getRecruitCount(),
                post.getRecruitCapacity(),
                recruitmentStatus,
                companionTypeText(post.getCompanionType()),
                ageConditionText(post),
                createdDate(post.getCreatedAt()),
                post.getViewCount(),
                tags == null ? List.of() : List.copyOf(tags),
                description
        );
    }

    private static String tripDuration(LocalDate startDate, LocalDate endDate) {
        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        return "%d박 %d일".formatted(nights, nights + 1);
    }

    private static String companionTypeText(CompanionType companionType) {
        if (companionType == null) {
            return "동행";
        }
        return switch (companionType) {
            case FULL -> "전체 동행";
            case PARTIAL -> "부분 동행";
            case MEAL -> "식사 동행";
            case UNSPECIFIED -> "동행";
        };
    }

    private static String ageConditionText(Post post) {
        String age = post.isAgeAny() ? "연령 무관" : "%d-%d세".formatted(post.getMinAge(), post.getMaxAge());
        return age + " · " + genderText(post.getPreferredGender());
    }

    private static String genderText(Gender gender) {
        if (gender == null) {
            return "성별 무관";
        }
        return switch (gender) {
            case M -> "남성";
            case F -> "여성";
            case U -> "성별 무관";
        };
    }

    private static String createdDate(LocalDateTime createdAt) {
        return createdAt == null ? "" : createdAt.format(CREATED_DATE_FORMATTER);
    }

    private static boolean isRecruitmentClosed(Post post, LocalDate today) {
        return post.isClosed()
                || post.isFull()
                || post.hasRecruitDeadlinePassed(today)
                || post.hasTravelStarted(today);
    }
}
