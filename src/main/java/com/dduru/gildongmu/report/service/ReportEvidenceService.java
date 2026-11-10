package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceResponse;
import com.dduru.gildongmu.report.dto.response.ReportEvidenceUploadResponse;
import com.dduru.gildongmu.report.exception.InvalidReportEvidenceException;
import com.dduru.gildongmu.report.exception.ReportEvidenceUnavailableException;
import com.dduru.gildongmu.report.exception.ReportNotFoundException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import com.dduru.gildongmu.s3.validation.ImageUploadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReportEvidenceService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(5);

    private final ObjectProvider<S3Presigner> presignerProvider;
    private final S3Properties s3Properties;
    private final ReportRepository reportRepository;

    public ReportEvidenceUploadResponse prepareUpload(Long userId, ImageUploadFileRequest file) {
        S3Presigner presigner = requirePresigner();
        ImageUploadValidator.validate(file);
        String extension = StringUtils.getFilenameExtension(file.fileName());
        String key = "reports/" + userId + "/" + UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentLength(file.fileSize())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_TTL)
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = presigner.presignPutObject(presignRequest).url().toString();
        return new ReportEvidenceUploadResponse(presignedUrl, key);
    }

    public void validateKey(Long userId, String key) {
        requirePresigner();
        String prefix = "reports/" + userId + "/";
        if (key == null || !key.matches(Pattern.quote(prefix)
                + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png)")) {
            throw new InvalidReportEvidenceException();
        }
    }

    @Transactional(readOnly = true)
    public ReportEvidenceResponse retrieveEvidence(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(ReportNotFoundException::new);
        if (report.getEvidenceKey() == null) {
            throw new InvalidReportEvidenceException();
        }
        S3Presigner presigner = requirePresigner();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(report.getEvidenceKey())
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_TTL)
                .getObjectRequest(getObjectRequest)
                .build();

        String presignedUrl = presigner.presignGetObject(presignRequest).url().toString();
        return new ReportEvidenceResponse(presignedUrl);
    }

    private S3Presigner requirePresigner() {
        S3Presigner presigner = presignerProvider.getIfAvailable();
        if (presigner == null || !StringUtils.hasText(s3Properties.getBucket())) {
            throw new ReportEvidenceUnavailableException();
        }
        return presigner;
    }
}
