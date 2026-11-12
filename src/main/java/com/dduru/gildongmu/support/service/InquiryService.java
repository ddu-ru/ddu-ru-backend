package com.dduru.gildongmu.support.service;

import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import com.dduru.gildongmu.support.domain.Inquiry;
import com.dduru.gildongmu.support.dto.request.*;
import com.dduru.gildongmu.support.dto.response.*;
import com.dduru.gildongmu.support.exception.InquiryNotFoundException;
import com.dduru.gildongmu.support.repository.InquiryRepository;
import com.dduru.gildongmu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final S3ImageUrlValidator imageUrlValidator;

    public InquiryListResponse list(Long userId, InquiryListRequest request) {
        int size = request.sizeOrDefault();
        return InquiryListResponse.of(inquiryRepository.findMine(userId, request.cursor(), PageRequest.of(0, size + 1)), size);
    }

    public InquiryDetailResponse detail(Long id, Long userId) {
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow(InquiryNotFoundException::new);
        inquiry.requireOwner(userId);
        return InquiryDetailResponse.from(inquiry);
    }

    @Transactional
    public InquiryDetailResponse create(Long userId, InquiryWriteRequest request) {
        Inquiry.validate(request.category(), request.content(), request.imageUrls());
        List<String> images = normalizeImages(request.imageUrls());
        Inquiry inquiry = Inquiry.create(userRepository.getByIdOrThrow(userId), request.category(), request.content(), images);
        return InquiryDetailResponse.from(inquiryRepository.saveAndFlush(inquiry));
    }

    @Transactional
    public InquiryDetailResponse update(Long id, Long userId, InquiryWriteRequest request) {
        Inquiry inquiry = inquiryRepository.findByIdWithLock(id).orElseThrow(InquiryNotFoundException::new);
        inquiry.requireOwner(userId);
        Inquiry.validate(request.category(), request.content(), request.imageUrls());
        inquiry.update(request.category(), request.content(), normalizeImages(request.imageUrls()));
        inquiryRepository.flush();
        return InquiryDetailResponse.from(inquiry);
    }

    private List<String> normalizeImages(List<String> images) {
        return images.stream()
                .map(url -> imageUrlValidator.validateAndNormalize(url, S3ImageDirectory.INQUIRIES, 2048))
                .toList();
    }
}
