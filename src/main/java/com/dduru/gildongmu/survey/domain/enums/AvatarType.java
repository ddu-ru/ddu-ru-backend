package com.dduru.gildongmu.survey.domain.enums;

import com.dduru.gildongmu.common.enums.CodedEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AvatarType implements CodedEnum {
    TTUR_POGUN(1),
    TTUR_POSEUL(2),
    TTUR_TTORANG(3),
    TTUR_DASOM(4),
    TTUR_MUDI(5),
    TTUR_SODAM(6),
    TTUR_SWEET(7),
    TTUR_DUDU(8),
    TTUR_SPARK(9),
    TTUR_LUNA(10),
    TTUR_GLIM(11),
    TTUR_HARAM(12),
    TTUR_MALLANG(13),
    TTUR_BONGBONG(14),
    TTUR_BEOMI(15),
    TTUR_MARU(16);

    private final int code;
}
