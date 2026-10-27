package com.dduru.gildongmu.survey.service;

import com.dduru.gildongmu.survey.domain.enums.AvatarType;
import org.springframework.stereotype.Component;

@Component
public class AvatarMatcher {

    private static final double THRESHOLD = 5.0;

    public AvatarType match(double rhythmScore, double energyScore, double consumptionScore, double decisionScore) {
        boolean isAdventurous = rhythmScore >= THRESHOLD;
        boolean isEnergetic = energyScore >= THRESHOLD;
        boolean isFlex = consumptionScore >= THRESHOLD;
        boolean isSocial = decisionScore >= THRESHOLD;

        if (!isEnergetic) {
            if (!isAdventurous && !isSocial && !isFlex) return AvatarType.TTUR_POGUN;
            if (!isAdventurous && !isSocial) return AvatarType.TTUR_POSEUL;
            if (!isAdventurous && !isFlex) return AvatarType.TTUR_TTORANG;
            if (!isAdventurous) return AvatarType.TTUR_DASOM;
            if (!isSocial && !isFlex) return AvatarType.TTUR_MUDI;
            if (!isSocial) return AvatarType.TTUR_SODAM;
            if (!isFlex) return AvatarType.TTUR_SWEET;
            return AvatarType.TTUR_DUDU;
        }

        if (!isAdventurous && !isSocial && !isFlex) return AvatarType.TTUR_SPARK;
        if (!isAdventurous && !isSocial) return AvatarType.TTUR_LUNA;
        if (!isAdventurous && !isFlex) return AvatarType.TTUR_GLIM;
        if (!isAdventurous) return AvatarType.TTUR_HARAM;
        if (!isSocial && !isFlex) return AvatarType.TTUR_MALLANG;
        if (!isSocial) return AvatarType.TTUR_BONGBONG;
        if (!isFlex) return AvatarType.TTUR_BEOMI;
        return AvatarType.TTUR_MARU;
    }
}
