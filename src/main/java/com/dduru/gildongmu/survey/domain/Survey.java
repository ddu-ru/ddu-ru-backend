package com.dduru.gildongmu.survey.domain;

import com.dduru.gildongmu.common.entity.BaseTimeEntity;
import com.dduru.gildongmu.user.domain.User;
import com.dduru.gildongmu.survey.domain.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey extends BaseTimeEntity {

    private static final int MAIN_AXIS_MAX_RAW = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "last_retested_at")
    private LocalDate lastRetestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rhythm_q1", nullable = false, length = 64)
    private RhythmQuestion1 rhythmQ1;

    @Enumerated(EnumType.STRING)
    @Column(name = "rhythm_q2", nullable = false, length = 64)
    private RhythmQuestion2 rhythmQ2;

    @Enumerated(EnumType.STRING)
    @Column(name = "rhythm_q3", nullable = false, length = 64)
    private RhythmQuestion3 rhythmQ3;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_q1", nullable = false, length = 64)
    private ConsumptionQuestion1 consumptionQ1;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_q2", nullable = false, length = 64)
    private ConsumptionQuestion2 consumptionQ2;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumption_q3", nullable = false, length = 64)
    private ConsumptionQuestion3 consumptionQ3;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_q1", nullable = false, length = 64)
    private EnergyQuestion1 energyQ1;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_q2", nullable = false, length = 64)
    private EnergyQuestion2 energyQ2;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_q3", nullable = false, length = 64)
    private EnergyQuestion3 energyQ3;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_q1", nullable = false, length = 64)
    private DecisionQuestion1 decisionQ1;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_q2", nullable = false, length = 64)
    private DecisionQuestion2 decisionQ2;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_q3", nullable = false, length = 64)
    private DecisionQuestion3 decisionQ3;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_style", nullable = false, length = 64)
    private RecordStyleQuestion recordStyle;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "survey_activity_tags", joinColumns = @JoinColumn(name = "survey_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 64)
    private List<ActivityTag> activityTags;

    @Builder
    public Survey(User user,
                  RhythmQuestion1 rhythmQ1, RhythmQuestion2 rhythmQ2, RhythmQuestion3 rhythmQ3,
                  ConsumptionQuestion1 consumptionQ1, ConsumptionQuestion2 consumptionQ2, ConsumptionQuestion3 consumptionQ3,
                  EnergyQuestion1 energyQ1, EnergyQuestion2 energyQ2, EnergyQuestion3 energyQ3,
                  DecisionQuestion1 decisionQ1, DecisionQuestion2 decisionQ2, DecisionQuestion3 decisionQ3,
                  RecordStyleQuestion recordStyle,
                  List<ActivityTag> activityTags) {
        this.user = user;
        this.rhythmQ1 = rhythmQ1;
        this.rhythmQ2 = rhythmQ2;
        this.rhythmQ3 = rhythmQ3;
        this.consumptionQ1 = consumptionQ1;
        this.consumptionQ2 = consumptionQ2;
        this.consumptionQ3 = consumptionQ3;
        this.energyQ1 = energyQ1;
        this.energyQ2 = energyQ2;
        this.energyQ3 = energyQ3;
        this.decisionQ1 = decisionQ1;
        this.decisionQ2 = decisionQ2;
        this.decisionQ3 = decisionQ3;
        this.recordStyle = recordStyle;
        this.activityTags = activityTags != null ? new ArrayList<>(activityTags) : new ArrayList<>();
    }

    public static Survey createSurvey(User user,
                                      RhythmQuestion1 rhythmQ1, RhythmQuestion2 rhythmQ2, RhythmQuestion3 rhythmQ3,
                                      ConsumptionQuestion1 consumptionQ1, ConsumptionQuestion2 consumptionQ2, ConsumptionQuestion3 consumptionQ3,
                                      EnergyQuestion1 energyQ1, EnergyQuestion2 energyQ2, EnergyQuestion3 energyQ3,
                                      DecisionQuestion1 decisionQ1, DecisionQuestion2 decisionQ2, DecisionQuestion3 decisionQ3,
                                      RecordStyleQuestion recordStyle,
                                      List<ActivityTag> activityTags) {
        return Survey.builder()
                .user(user)
                .rhythmQ1(rhythmQ1)
                .rhythmQ2(rhythmQ2)
                .rhythmQ3(rhythmQ3)
                .consumptionQ1(consumptionQ1)
                .consumptionQ2(consumptionQ2)
                .consumptionQ3(consumptionQ3)
                .energyQ1(energyQ1)
                .energyQ2(energyQ2)
                .energyQ3(energyQ3)
                .decisionQ1(decisionQ1)
                .decisionQ2(decisionQ2)
                .decisionQ3(decisionQ3)
                .recordStyle(recordStyle)
                .activityTags(activityTags)
                .build();
    }

    public void updateSurvey(RhythmQuestion1 rhythmQ1, RhythmQuestion2 rhythmQ2, RhythmQuestion3 rhythmQ3,
                             ConsumptionQuestion1 consumptionQ1, ConsumptionQuestion2 consumptionQ2, ConsumptionQuestion3 consumptionQ3,
                             EnergyQuestion1 energyQ1, EnergyQuestion2 energyQ2, EnergyQuestion3 energyQ3,
                             DecisionQuestion1 decisionQ1, DecisionQuestion2 decisionQ2, DecisionQuestion3 decisionQ3,
                             RecordStyleQuestion recordStyle,
                             List<ActivityTag> activityTags) {
        this.rhythmQ1 = rhythmQ1;
        this.rhythmQ2 = rhythmQ2;
        this.rhythmQ3 = rhythmQ3;
        this.consumptionQ1 = consumptionQ1;
        this.consumptionQ2 = consumptionQ2;
        this.consumptionQ3 = consumptionQ3;
        this.energyQ1 = energyQ1;
        this.energyQ2 = energyQ2;
        this.energyQ3 = energyQ3;
        this.decisionQ1 = decisionQ1;
        this.decisionQ2 = decisionQ2;
        this.decisionQ3 = decisionQ3;
        this.recordStyle = recordStyle;
        this.activityTags = activityTags != null ? new ArrayList<>(activityTags) : new ArrayList<>();
    }

    public static int mainAxisMaxRaw() {
        return MAIN_AXIS_MAX_RAW;
    }

    public void recordRetest(LocalDate testedAt) {
        this.lastRetestedAt = testedAt;
    }
}
