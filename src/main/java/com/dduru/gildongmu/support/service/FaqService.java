package com.dduru.gildongmu.support.service;

import com.dduru.gildongmu.support.domain.Faq;
import com.dduru.gildongmu.support.dto.request.FaqListRequest;
import com.dduru.gildongmu.support.dto.response.FaqResponse;
import com.dduru.gildongmu.support.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqResponse> list(FaqListRequest request) {
        Specification<Faq> spec = (root, query, cb) -> cb.isTrue(root.get("visible"));
        if (request.category() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), request.category()));
        }
        if (request.keyword() != null) {
            String pattern = "%" + request.keyword().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("question")), pattern, '!'),
                    cb.like(cb.lower(root.get("answer")), pattern, '!')));
        }
        return faqRepository.findAll(spec, Sort.by("sortOrder", "id")).stream()
                .map(FaqResponse::from)
                .toList();
    }
}
