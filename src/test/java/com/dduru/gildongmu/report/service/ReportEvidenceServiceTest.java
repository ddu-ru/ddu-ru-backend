package com.dduru.gildongmu.report.service;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.report.domain.Report;
import com.dduru.gildongmu.report.exception.InvalidReportEvidenceException;
import com.dduru.gildongmu.report.exception.ReportEvidenceUnavailableException;
import com.dduru.gildongmu.report.repository.ReportRepository;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import com.dduru.gildongmu.s3.exception.InvalidFileExtensionException;
import com.dduru.gildongmu.s3.exception.InvalidFileSizeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportEvidenceService 테스트")
class ReportEvidenceServiceTest {

    @Mock
    private ObjectProvider<S3Presigner> presignerProvider;

    @Mock
    private S3Presigner presigner;

    @Mock
    private S3Properties s3Properties;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportEvidenceService reportEvidenceService;

    @Test
    @DisplayName("서명기가 없으면 증빙 업로드 URL을 발급하지 않는다")
    void rejectsMissingPresigner() {
        assertThatThrownBy(() -> reportEvidenceService.prepareUpload(1L, new ImageUploadFileRequest("a.png", 10L)))
                .isInstanceOf(ReportEvidenceUnavailableException.class);
    }

    @Test
    @DisplayName("버킷 설정이 없으면 증빙 기능을 사용할 수 없다")
    void rejectsMissingBucket() {
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(s3Properties.getBucket()).thenReturn("");
        assertThatThrownBy(() -> reportEvidenceService.validateKey(1L, "anything"))
                .isInstanceOf(ReportEvidenceUnavailableException.class);
    }

    @Test
    @DisplayName("본인 경로의 정규 증빙 키만 허용한다")
    void onlyAcceptsOwnCanonicalEvidenceKey() {
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(s3Properties.getBucket()).thenReturn("dduru26-v2");
        String suffix = "12345678-1234-1234-1234-123456789abc.png";
        assertThatCode(() -> reportEvidenceService.validateKey(1L, "reports/1/" + suffix)).doesNotThrowAnyException();
        assertThatThrownBy(() -> reportEvidenceService.validateKey(1L, "reports/2/" + suffix)).isInstanceOf(InvalidReportEvidenceException.class);
        assertThatThrownBy(() -> reportEvidenceService.validateKey(1L, "reports/1/../2/" + suffix)).isInstanceOf(InvalidReportEvidenceException.class);
        assertThatThrownBy(() -> reportEvidenceService.validateKey(1L, "https://example.com/a.png")).isInstanceOf(InvalidReportEvidenceException.class);
    }

    @Test
    @DisplayName("5MB 초과 파일과 GIF를 거부한다")
    void rejectsOversizeAndGif() {
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(s3Properties.getBucket()).thenReturn("dduru26-v2");
        assertThatThrownBy(() -> reportEvidenceService.prepareUpload(1L, new ImageUploadFileRequest("a.png", 5_242_881L)))
                .isInstanceOf(InvalidFileSizeException.class);
        assertThatThrownBy(() -> reportEvidenceService.prepareUpload(1L, new ImageUploadFileRequest("a.gif", 10L)))
                .isInstanceOf(InvalidFileExtensionException.class);
    }

    @Test
    @DisplayName("기존 버킷의 본인 경로에 파일 크기를 지정한 업로드 URL을 발급한다")
    void signsUploadInExistingBucketWithExactLengthAndOwnerPrefix() throws Exception {
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(s3Properties.getBucket()).thenReturn("dduru26-v2");
        PresignedPutObjectRequest signed = mock(PresignedPutObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://private.example.com/upload").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(signed);
        var response = reportEvidenceService.prepareUpload(1L, new ImageUploadFileRequest("a.PNG", 5_242_880L));
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("dduru26-v2");
        assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(5_242_880L);
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(response.evidenceKey()).startsWith("reports/1/").endsWith(".png");
    }

    @Test
    @DisplayName("저장된 증빙 키에 대해 5분짜리 조회 URL을 발급한다")
    void signsDownloadForSavedEvidenceInExistingBucket() throws Exception {
        when(presignerProvider.getIfAvailable()).thenReturn(presigner);
        when(s3Properties.getBucket()).thenReturn("dduru26-v2");
        Report report = mock(Report.class);
        String key = "reports/1/12345678-1234-1234-1234-123456789abc.png";
        when(report.getEvidenceKey()).thenReturn(key);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://example.com/signed-download").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);

        var response = reportEvidenceService.retrieveEvidence(10L);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("dduru26-v2");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo(key);
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(response.downloadUrl()).isEqualTo("https://example.com/signed-download");
    }
}
