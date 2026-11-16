package com.dduru.gildongmu.profile.dto.response;

import com.dduru.gildongmu.common.util.JsonConverter;
import com.dduru.gildongmu.survey.domain.AvatarProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "마이페이지 여행 스타일 요약")
public record TravelStyleSummaryResponse(
        @Schema(description = "여행 스타일 이름", example = "스윗")
        String name,
        @Schema(description = "여행 스타일 한줄 설명", example = "좋은 건 무조건 다 경험해봐야죠!")
        String description,
        @Schema(description = "여행 스타일 태그", example = "[\"열정적\", \"도전정신\", \"적극적\"]")
        List<String> tags
) {
    public static TravelStyleSummaryResponse from(AvatarProfile avatarProfile, JsonConverter jsonConverter) {
        if (avatarProfile == null) {
            return null;
        }
        return new TravelStyleSummaryResponse(
                avatarProfile.getDisplayName(),
                avatarProfile.getSpeechBubbleText(),
                jsonConverter.convertJsonToList(avatarProfile.getTags())
        );
    }
}
