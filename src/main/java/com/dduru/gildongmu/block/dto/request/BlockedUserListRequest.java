package com.dduru.gildongmu.block.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record BlockedUserListRequest(
        @Positive Long cursor,
        @Min(1) @Max(50) Integer size
) {
    public int pageSize() {
        return size == null ? 20 : size;
    }
}
