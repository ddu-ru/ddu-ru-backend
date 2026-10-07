package com.dduru.gildongmu.s3.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ImageUploadFileRequest(
        @Schema(description = "원본 파일명. jpg, jpeg, png 확장자를 허용합니다.", example = "trip-cover.jpg")
        @NotBlank(message = "파일명은 비어있을 수 없습니다")
        String fileName,
        @Schema(description = "실제 업로드할 파일 크기(바이트). 최대 5,242,880바이트입니다.", example = "1048576")
        @NotNull(message = "파일 크기는 필수입니다")
        @Positive(message = "파일 크기는 0보다 커야 합니다")
        @Max(value = ImageUploadFileRequest.MAX_FILE_SIZE, message = "이미지는 파일당 최대 5MB까지 업로드 가능합니다")
        Long fileSize
) {
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
}
