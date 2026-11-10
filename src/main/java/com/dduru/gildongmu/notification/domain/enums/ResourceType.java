package com.dduru.gildongmu.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    JOURNEY("JOURNEY"),
    JOURNEY_POST("JOURNEY_POST"),
    SCHEDULE("SCHEDULE"),
    MATCH("MATCH"),
    POST("POST"),
    CHAT_ROOM("CHAT_ROOM"),
    REPORT("REPORT");

    private final String payloadValue;
}
