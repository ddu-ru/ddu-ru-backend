package com.dduru.gildongmu.s3.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUploadRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 5242881L})
    void invalidSize_rejectedThroughNestedValidation(Long fileSize) {
        ImageUploadRequest request = new ImageUploadRequest(List.of(new ImageUploadFileRequest("photo.jpg", fileSize)));
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 5242880L})
    void validSize_acceptedIncludingBoundaries(long fileSize) {
        ImageUploadRequest request = new ImageUploadRequest(List.of(new ImageUploadFileRequest("photo.jpg", fileSize)));
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void missingOrEmptyFiles_rejected() {
        assertThat(validator.validate(new ImageUploadRequest(null))).isNotEmpty();
        assertThat(validator.validate(new ImageUploadRequest(List.of()))).isNotEmpty();
        assertThat(validator.validate(new ImageUploadRequest(Collections.singletonList(null)))).isNotEmpty();
    }

    @Test
    void blankFileName_rejected() {
        assertThat(validator.validate(new ImageUploadRequest(List.of(new ImageUploadFileRequest(" ", 1024L)))))
                .isNotEmpty();
    }

    @Test
    void fileCount_allowsTenButRejectsEleven() {
        ImageUploadFileRequest file = new ImageUploadFileRequest("photo.jpg", 1024L);
        assertThat(validator.validate(new ImageUploadRequest(Collections.nCopies(10, file)))).isEmpty();
        assertThat(validator.validate(new ImageUploadRequest(Collections.nCopies(11, file)))).isNotEmpty();
    }
}
