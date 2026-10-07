package com.dduru.gildongmu.common.validation;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.common.exception.ErrorCode;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("S3 이미지 URL validator 테스트")
class S3ImageUrlValidatorTest {

    private S3ImageUrlValidator validator;

    @BeforeEach
    void setUp() {
        S3Properties s3Properties = new S3Properties();
        s3Properties.setBucket("dummy-bucket");
        s3Properties.setRegion("ap-northeast-2");
        validator = new S3ImageUrlValidator(s3Properties);
    }

    @Test
    @DisplayName("우리 S3 chats 경로 URL이면 허용한다")
    void validateAndNormalize_validChatS3Url_returnsUrl() {
        String url = "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/chats/123e4567-e89b-12d3-a456-426614174000.JPG";

        String result = validator.validateAndNormalize(url, S3ImageDirectory.CHATS);

        assertThat(result).isEqualTo(url);
    }

    @Test
    @DisplayName("우리 S3 journeys 경로 URL이면 허용한다")
    void validateAndNormalize_validJourneyS3Url_returnsUrl() {
        String url = "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/journeys/test.jpg";

        String result = validator.validateAndNormalize(url, S3ImageDirectory.JOURNEYS);

        assertThat(result).isEqualTo(url);
    }

    @Test
    @DisplayName("외부 호스트 URL이면 거부한다")
    void validateAndNormalize_externalHost_throwsNotAllowed() {
        assertThatThrownBy(() -> validator.validateAndNormalize("https://example.com/chats/test.jpg", S3ImageDirectory.CHATS))
                .hasMessage(ErrorCode.IMAGE_URL_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("S3 URL여도 chats 경로가 아니면 거부한다")
    void validateAndNormalize_nonChatsPath_throwsNotAllowed() {
        assertThatThrownBy(() -> validator.validateAndNormalize(
                "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/profiles/test.jpg",
                S3ImageDirectory.CHATS
        )).hasMessage(ErrorCode.IMAGE_URL_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("presigned URL처럼 query가 포함되면 거부한다")
    void validateAndNormalize_presignedUrlWithQuery_throwsNotAllowed() {
        assertThatThrownBy(() -> validator.validateAndNormalize(
                "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/chats/test.jpg?X-Amz-Signature=abc",
                S3ImageDirectory.CHATS
        )).hasMessage(ErrorCode.IMAGE_URL_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("허용되지 않는 이미지 확장자면 거부한다")
    void validateAndNormalize_invalidImageExtension_throwsNotAllowed() {
        assertThatThrownBy(() -> validator.validateAndNormalize(
                "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/journeys/test.txt",
                S3ImageDirectory.JOURNEYS
        )).hasMessage(ErrorCode.IMAGE_URL_NOT_ALLOWED.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"gif", "GIF"})
    void validateAndNormalize_gif_rejected(String extension) {
        assertThatThrownBy(() -> validator.validateAndNormalize(
                "https://dummy-bucket.s3.ap-northeast-2.amazonaws.com/journeys/test." + extension,
                S3ImageDirectory.JOURNEYS
        )).hasMessage(ErrorCode.IMAGE_URL_NOT_ALLOWED.getMessage());
    }
}
