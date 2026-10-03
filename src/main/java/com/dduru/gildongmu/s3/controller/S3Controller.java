package com.dduru.gildongmu.s3.controller;

import com.dduru.gildongmu.s3.dto.request.ImageUploadRequest;
import com.dduru.gildongmu.s3.dto.response.ImageUploadResponse;
import com.dduru.gildongmu.s3.service.S3Service;
import com.dduru.gildongmu.common.dto.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
public class S3Controller implements S3ApiDocs {
    private final S3Service s3Service;

    @Override
    @PostMapping("/posts/presigned-url")
    public ResponseEntity<ApiResult<List<ImageUploadResponse>>> preparePostImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = s3Service.preparePostImageUpload(request.files());
        return ResponseEntity.ok(ApiResult.ok(responses));
    }

    @Override
    @PostMapping("/profiles/presigned-url")
    public ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareProfileImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = s3Service.prepareProfileImageUpload(request.files());
        return ResponseEntity.ok(ApiResult.ok(responses));
    }

    @Override
    @PostMapping("/journeys/presigned-url")
    public ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareJourneyImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = s3Service.prepareJourneyImageUpload(request.files());
        return ResponseEntity.ok(ApiResult.ok(responses));
    }

    /*@Override
    @PostMapping("/surveys/uploads")
    public ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareSurveyImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = s3Service.prepareSurveyImageUpload(request.fileNames());
        return ResponseEntity.ok(ApiResult.ok(responses));
    }*/

    @Override
    @PostMapping("/chats/presigned-url")
    public ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareChatImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    ) {
        List<ImageUploadResponse> responses = s3Service.prepareChatImageUpload(request.files());
        return ResponseEntity.ok(ApiResult.ok(responses));
    }

}
