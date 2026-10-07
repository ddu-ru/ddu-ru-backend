package com.dduru.gildongmu.s3.service;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import com.dduru.gildongmu.s3.dto.response.ImageUploadResponse;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import com.dduru.gildongmu.s3.exception.InvalidFileExtensionException;
import com.dduru.gildongmu.s3.exception.InvalidFileSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /*private static final String S3_SURVEY_DIR = "survey/";*/
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(10);

    public List<ImageUploadResponse> preparePostImageUpload(List<ImageUploadFileRequest> files) {
        List<ImageUploadResponse> responses = files.stream()
                .map(file -> prepareUploadInternal(file, S3ImageDirectory.POSTS))
                .toList();
        log.info("Presigned URL 생성 완료{} 파일 개수: {}", S3ImageDirectory.POSTS.keyPrefix(), responses.size());
        return responses;
    }


    public List<ImageUploadResponse> prepareProfileImageUpload(List<ImageUploadFileRequest> files) {
        List<ImageUploadResponse> responses = files.stream()
                .map(file -> prepareUploadInternal(file, S3ImageDirectory.PROFILES))
                .toList();
        log.info("Presigned URL 생성 완료{} 파일 개수: {}", S3ImageDirectory.PROFILES.keyPrefix(), responses.size());
        return responses;
    }

    public List<ImageUploadResponse> prepareJourneyImageUpload(List<ImageUploadFileRequest> files) {
        List<ImageUploadResponse> responses = files.stream()
                .map(file -> prepareUploadInternal(file, S3ImageDirectory.JOURNEYS))
                .toList();
        log.info("Presigned URL 생성 완료{} 파일 개수: {}", S3ImageDirectory.JOURNEYS.keyPrefix(), responses.size());
        return responses;
    }

   /* public List<ImageUploadResponse> prepareSurveyImageUpload(List<String> fileNames) {
        log.debug("Presigned URL 생성 시작(survey) - 파일 개수: {}", fileNames.size());
        List<ImageUploadResponse> responses = fileNames.stream()
                .map(fileName -> prepareUploadInternal(fileName, S3_SURVEY_DIR, false))
                .toList();
        log.info("Presigned URL 생성 완료(survey) - 파일 개수: {}", responses.size());
        return responses;
    }*/


    public List<ImageUploadResponse> prepareChatImageUpload(List<ImageUploadFileRequest> files) {
        List<ImageUploadResponse> responses = files.stream()
                .map(file -> prepareUploadInternal(file, S3ImageDirectory.CHATS))
                .toList();
        log.debug("Presigned URL 생성 완료{} 파일 개수: {}", S3ImageDirectory.CHATS.keyPrefix(), responses.size());
        return responses;
    }

    private ImageUploadResponse prepareUploadInternal(ImageUploadFileRequest file, S3ImageDirectory directory) {
        validateFileExtension(file.fileName());
        if (file.fileSize() == null || file.fileSize() <= 0 || file.fileSize() > ImageUploadFileRequest.MAX_FILE_SIZE) {
            throw new InvalidFileSizeException();
        }
        String finalFileName = generateFileName(file.fileName());
        String key = directory.keyPrefix() + finalFileName;
        return presignPut(key, file.fileSize());
    }

    public String getS3Url(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(s3Properties.getBucket(), s3Properties.getRegion(), key);
    }

    private ImageUploadResponse presignPut(String key, long fileSize) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentLength(fileSize)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = getS3Url(key);
        return new ImageUploadResponse(presignedUrl, fileUrl);
    }

    private void validateFileExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        List<String> allowedExtensions = S3ImageUrlValidator.allowedFileExtensions();
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw InvalidFileExtensionException.invalidExtension(allowedExtensions);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return UUID.randomUUID() + "." + extension;
    }
}
