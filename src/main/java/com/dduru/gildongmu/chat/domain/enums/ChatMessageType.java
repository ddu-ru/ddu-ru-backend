package com.dduru.gildongmu.chat.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 타입. TEXT는 텍스트, IMAGE는 이미지, POLL은 투표 카드, SYSTEM은 시스템 메시지입니다.")
public enum ChatMessageType {
    TEXT,
    IMAGE,
    POLL,
    SYSTEM
}
