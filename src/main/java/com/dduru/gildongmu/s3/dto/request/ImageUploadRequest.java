package com.dduru.gildongmu.s3.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImageUploadRequest(
        @Schema(description = "업로드할 파일 목록. JPG, JPEG, PNG만 허용하며 파일당 최대 5MB(5,242,880바이트)입니다.")
        @NotEmpty(message = "파일 목록은 필수입니다")
        @Size(max = 10, message = "한 번에 최대 10개까지 업로드 가능합니다")
        List<@NotNull(message = "파일 정보는 필수입니다") @Valid ImageUploadFileRequest> files
) {
}
