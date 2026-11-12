package com.dduru.gildongmu.support.dto.response;

import com.dduru.gildongmu.support.domain.Faq;
import com.dduru.gildongmu.support.domain.enums.FaqCategory;

public record FaqResponse(
        Long id,
        FaqCategory category,
        String question,
        String answer
) {
    public static FaqResponse from(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getCategory(),
                faq.getQuestion(),
                faq.getAnswer()
        );
    }
}
