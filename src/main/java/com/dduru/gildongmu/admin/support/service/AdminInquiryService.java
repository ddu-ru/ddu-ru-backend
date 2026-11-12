package com.dduru.gildongmu.admin.support.service;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.dto.response.AdminInquiryListResponse;
import com.dduru.gildongmu.common.time.TimeProvider;
import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.domain.enums.InquiryStatus;
import com.dduru.gildongmu.support.dto.response.InquiryDetailResponse;
import com.dduru.gildongmu.support.exception.InquiryNotFoundException;
import com.dduru.gildongmu.support.repository.InquiryRepository;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final TimeProvider timeProvider;

    public AdminInquiryListResponse findAll(InquiryStatus status, Pageable pageable) {
        Pageable ordered = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Direction.DESC, "id")
        );
        return AdminInquiryListResponse.from(inquiryRepository.findByOptionalStatus(status, ordered));
    }

    public InquiryDetailResponse findById(Long id) {
        return InquiryDetailResponse.from(
                inquiryRepository.findById(id).orElseThrow(InquiryNotFoundException::new)
        );
    }

    @Transactional
    public InquiryDetailResponse updateAnswer(
            Long id,
            Long adminId,
            InquiryAnswerRequest request
    ) {
        Inquiry inquiry = inquiryRepository.findByIdWithLock(id)
                .orElseThrow(InquiryNotFoundException::new);
        inquiry.answer(request.content(), userRepository.getByIdOrThrow(adminId), timeProvider.now());
        inquiryRepository.flush();
        return InquiryDetailResponse.from(inquiry);
    }
}
