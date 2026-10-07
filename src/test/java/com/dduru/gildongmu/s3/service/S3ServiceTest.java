package com.dduru.gildongmu.s3.service;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import com.dduru.gildongmu.s3.dto.response.ImageUploadResponse;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import com.dduru.gildongmu.s3.exception.InvalidFileExtensionException;
import com.dduru.gildongmu.s3.exception.InvalidFileSizeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class S3ServiceTest {

    private S3Presigner presigner;
    private S3Service service;

    @BeforeEach
    void setUp() {
        presigner = spy(S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                .build());
        S3Properties properties = new S3Properties();
        properties.setBucket("test-bucket");
        properties.setRegion("ap-northeast-2");
        service = new S3Service(presigner, properties);
    }

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @ParameterizedTest
    @EnumSource(value = S3ImageDirectory.class, names = {"POSTS", "PROFILES", "JOURNEYS", "CHATS"})
    void prepareUpload_allDirectories_signExactSizeAtLimit(S3ImageDirectory directory) {
        List<ImageUploadFileRequest> files = List.of(new ImageUploadFileRequest("photo.jpg", 5242880L));
        List<ImageUploadResponse> responses = switch (directory) {
            case POSTS -> service.preparePostImageUpload(files);
            case PROFILES -> service.prepareProfileImageUpload(files);
            case JOURNEYS -> service.prepareJourneyImageUpload(files);
            case CHATS -> service.prepareChatImageUpload(files);
            default -> throw new IllegalArgumentException("No upload endpoint for " + directory);
        };

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).fileUrl()).contains("/" + directory.keyPrefix());
        String decodedUrl = URLDecoder.decode(responses.get(0).presignedUrl(), StandardCharsets.UTF_8);
        assertThat(decodedUrl).contains("X-Amz-SignedHeaders=content-length;host");
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(5242880L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpg", "jpeg", "png", "JPG", "JPEG", "PNG"})
    void prepareUpload_supportedExtensions_succeeds(String extension) {
        assertThat(service.preparePostImageUpload(List.of(new ImageUploadFileRequest("photo." + extension, 1L))))
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gif", "GIF", "webp", "heic", "svg"})
    void prepareUpload_unsupportedExtensions_rejectedBeforeSigning(String extension) {
        assertThatThrownBy(() -> service.preparePostImageUpload(List.of(new ImageUploadFileRequest("photo." + extension, 1024L))))
                .isInstanceOf(InvalidFileExtensionException.class);
        verify(presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 5242881L})
    void prepareUpload_invalidSize_rejectedBeforeSigning(Long fileSize) {
        assertThatThrownBy(() -> service.preparePostImageUpload(List.of(new ImageUploadFileRequest("photo.jpg", fileSize))))
                .isInstanceOfSatisfying(InvalidFileSizeException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_SIZE));
        verify(presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
    }
}
