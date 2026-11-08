package com.dduru.gildongmu.s3.validation;

import com.dduru.gildongmu.common.validation.S3ImageUrlValidator;
import com.dduru.gildongmu.s3.dto.request.ImageUploadFileRequest;
import com.dduru.gildongmu.s3.exception.InvalidFileExtensionException;
import com.dduru.gildongmu.s3.exception.InvalidFileSizeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageUploadValidator {

    public static void validate(ImageUploadFileRequest file) {
        String extension = StringUtils.getFilenameExtension(file.fileName());
        List<String> allowedExtensions = S3ImageUrlValidator.allowedFileExtensions();
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw InvalidFileExtensionException.invalidExtension(allowedExtensions);
        }
        if (file.fileSize() == null || file.fileSize() <= 0 || file.fileSize() > ImageUploadFileRequest.MAX_FILE_SIZE) {
            throw new InvalidFileSizeException();
        }
    }
}
