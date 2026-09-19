package com.dduru.gildongmu.recommendation.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.destination.domain.Destination;
import com.dduru.gildongmu.recommendation.domain.enums.RecommendationDestinationPreferenceType;
import com.dduru.gildongmu.recommendation.exception.InvalidDestinationPreferenceException;
import com.dduru.gildongmu.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_recommendation_destination_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recommendation_destination_rank", columnNames = {"user_id", "preference_rank"}),
                @UniqueConstraint(name = "uk_recommendation_destination_country", columnNames = {"user_id", "preference_type", "country_code"}),
                @UniqueConstraint(name = "uk_recommendation_destination_city", columnNames = {"user_id", "preference_type", "destination_id"})
        }
)
@Check(constraints = "preference_rank BETWEEN 1 AND 3")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRecommendationDestinationPreference extends BaseTimeEntity {

    private static final int COUNTRY_CODE_LENGTH = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 20)
    private RecommendationDestinationPreferenceType preferenceType;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @Column(name = "preference_rank", nullable = false)
    private int preferenceRank;

    private UserRecommendationDestinationPreference(
            User user,
            RecommendationDestinationPreferenceType preferenceType,
            String countryCode,
            Destination destination,
            int preferenceRank
    ) {
        if (preferenceRank < 1 || preferenceRank > 3) {
            throw new InvalidDestinationPreferenceException();
        }
        this.preferenceRank = preferenceRank;
        validatePreferenceValue(preferenceType, countryCode, destination);
        this.user = user;
        this.preferenceType = preferenceType;
        this.countryCode = countryCode;
        this.destination = destination;
    }

    public static UserRecommendationDestinationPreference country(User user, String countryCode, int preferenceRank) {
        return new UserRecommendationDestinationPreference(
                user,
                RecommendationDestinationPreferenceType.COUNTRY,
                countryCode,
                null,
                preferenceRank
        );
    }

    public static UserRecommendationDestinationPreference city(User user, Destination destination, int preferenceRank) {
        return new UserRecommendationDestinationPreference(
                user,
                RecommendationDestinationPreferenceType.CITY,
                null,
                destination,
                preferenceRank
        );
    }

    private static void validatePreferenceValue(
            RecommendationDestinationPreferenceType preferenceType,
            String countryCode,
            Destination destination
    ) {
        if (preferenceType == null) {
            throw new InvalidDestinationPreferenceException();
        }
        if (preferenceType == RecommendationDestinationPreferenceType.COUNTRY
                && (countryCode == null
                || countryCode.isBlank()
                || countryCode.length() != COUNTRY_CODE_LENGTH
                || destination != null)) {
            throw new InvalidDestinationPreferenceException();
        }
        if (preferenceType == RecommendationDestinationPreferenceType.CITY
                && (countryCode != null || destination == null)) {
            throw new InvalidDestinationPreferenceException();
        }
    }
}
