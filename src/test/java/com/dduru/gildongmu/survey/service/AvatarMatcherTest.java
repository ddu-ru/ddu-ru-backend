package com.dduru.gildongmu.survey.service;

import com.dduru.gildongmu.survey.domain.enums.AvatarType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아바타 매칭 테스트")
class AvatarMatcherTest {

    private final AvatarMatcher avatarMatcher = new AvatarMatcher();

    @ParameterizedTest(name = "[{index}] r={0}, e={1}, c={2}, d={3} -> {4}")
    @CsvSource({
            "4.0, 4.0, 4.0, 4.0, TTUR_POGUN",
            "4.0, 4.0, 6.0, 4.0, TTUR_POSEUL",
            "4.0, 4.0, 4.0, 6.0, TTUR_TTORANG",
            "4.0, 4.0, 6.0, 6.0, TTUR_DASOM",
            "6.0, 4.0, 4.0, 4.0, TTUR_MUDI",
            "6.0, 4.0, 6.0, 4.0, TTUR_SODAM",
            "6.0, 4.0, 4.0, 6.0, TTUR_SWEET",
            "6.0, 4.0, 6.0, 6.0, TTUR_DUDU",
            "4.0, 6.0, 4.0, 4.0, TTUR_SPARK",
            "4.0, 6.0, 6.0, 4.0, TTUR_LUNA",
            "4.0, 6.0, 4.0, 6.0, TTUR_GLIM",
            "4.0, 6.0, 6.0, 6.0, TTUR_HARAM",
            "6.0, 6.0, 4.0, 4.0, TTUR_MALLANG",
            "6.0, 6.0, 6.0, 4.0, TTUR_BONGBONG",
            "6.0, 6.0, 4.0, 6.0, TTUR_BEOMI",
            "6.0, 6.0, 6.0, 6.0, TTUR_MARU"
    })
    @DisplayName("4축 조합별 아바타 매칭")
    void 사축_조합별_아바타_매칭(double rhythm, double energy, double consumption, double decision, AvatarType expected) {
        AvatarType avatar = avatarMatcher.match(rhythm, energy, consumption, decision);
        assertThat(avatar).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] r={0}, e={1}, c={2}, d={3} -> {4}")
    @CsvSource({
            "5.0, 5.0, 5.0, 5.0, TTUR_MARU",
            "4.9, 4.9, 4.9, 4.9, TTUR_POGUN",
            "4.9, 5.0, 4.9, 4.9, TTUR_SPARK",
            "5.0, 4.9, 4.9, 5.0, TTUR_SWEET"
    })
    @DisplayName("임계값 5.0 경계 매칭")
    void 임계값_경계_매칭(double rhythm, double energy, double consumption, double decision, AvatarType expected) {
        AvatarType avatar = avatarMatcher.match(rhythm, energy, consumption, decision);
        assertThat(avatar).isEqualTo(expected);
    }
}
