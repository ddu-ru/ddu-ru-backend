package com.dduru.gildongmu.report.controller;

import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceUploadResponse;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Reports")
@SecurityRequirement(name = "JWT")
public interface ReportEvidenceApiDocs {

    @Operation(summary = "신고 증빙 사진 업로드 URL 발급", description = "선택 1장, JPG/JPEG/PNG, 최대 5,242,880바이트. "
            + "기존 S3 버킷의 비공개 reports/ 경로에 PUT 업로드 후 evidenceKey를 신고 요청에 전달합니다. URL은 10분간 유효합니다.")
    ResponseEntity<ApiResult<ReportEvidenceUploadResponse>> prepareEvidenceUpload(
            @Parameter(hidden = true) Long userId,
            ImageUploadFileRequest request
    );
}
