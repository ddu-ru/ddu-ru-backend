package com.dduru.gildongmu.recommendation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelPreferenceUpdateRequest(
        @NotNull @Size(max = 3) List<@NotNull @Valid DestinationPreferenceRequest> destinationPreferences,
        @NotNull List<@NotNull @Valid AvailableDateRequest> availableDates
) {
}
