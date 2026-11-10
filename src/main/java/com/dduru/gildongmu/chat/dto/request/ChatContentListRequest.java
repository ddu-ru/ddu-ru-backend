package com.dduru.gildongmu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Schema(description = "채팅방 사진·투표 모아보기 조회 조건")
public record ChatContentListRequest(
        @Schema(description = "이전 응답의 nextCursor. 사진·투표 ID가 아닌 메시지 ID이며 첫 페이지는 생략", example = "120")
        @Positive(message = "cursor는 1 이상이어야 합니다.")
        Long cursor,

        @Schema(description = "페이지 크기. 생략 시 20, 최대 50. 미리보기에도 같은 API 사용", example = "5")
        @Min(value = 1, message = "size는 1 이상 50 이하로 입력해야 합니다.")
        @Max(value = 50, message = "size는 1 이상 50 이하로 입력해야 합니다.")
        Integer size
) {
    public int sizeOrDefault() {
        return size == null ? ChatMessageRetrieveRequest.DEFAULT_SIZE : size;
    }
}
