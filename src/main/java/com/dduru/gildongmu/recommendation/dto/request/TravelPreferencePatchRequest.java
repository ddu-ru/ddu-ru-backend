package com.dduru.gildongmu.recommendation.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "생략한 목록은 유지하고 전달한 목록만 전체 교체합니다. 빈 배열은 삭제, 명시적 null은 허용하지 않습니다.")
public class TravelPreferencePatchRequest {

    @Size(max = 3)
    @Schema(description = "배열 순서가 선호 순위입니다. 생략하면 기존 여행지를 유지합니다.")
    private List<@NotNull @Valid DestinationPreferenceRequest> destinationPreferences;

    @Schema(description = "생략하면 기존 여행 가능 날짜를 유지합니다.")
    private List<@NotNull @Valid AvailableDateRequest> availableDates;

    @JsonSetter(nulls = Nulls.FAIL)
    public void setDestinationPreferences(List<DestinationPreferenceRequest> destinationPreferences) {
        this.destinationPreferences = destinationPreferences;
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setAvailableDates(List<AvailableDateRequest> availableDates) {
        this.availableDates = availableDates;
    }
}
