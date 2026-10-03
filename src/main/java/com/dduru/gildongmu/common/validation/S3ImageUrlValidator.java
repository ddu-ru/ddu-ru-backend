package com.dduru.gildongmu.common.validation;

import com.dduru.gildongmu.common.config.S3Properties;
import com.dduru.gildongmu.s3.enums.S3ImageDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class S3ImageUrlValidator {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png");

    private final S3Properties s3Properties;

    public String validateAndNormalize(String raw, S3ImageDirectory directory) {
        return validateAndNormalize(raw, directory, null);
    }

    public String validateAndNormalize(String raw, S3ImageDirectory directory, Integer maxLength) {
        if (raw == null) {
            throw InvalidImageUrlException.missing();
        }

        String url = raw.trim();
        if (url.isEmpty()) {
            throw InvalidImageUrlException.blank();
        }
        if (maxLength != null && url.length() > maxLength) {
            throw InvalidImageUrlException.tooLong();
        }

        URI uri = parse(url);
        if (!uri.isAbsolute()) {
            throw InvalidImageUrlException.notAbsolute();
        }
        if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            throw InvalidImageUrlException.nonHttpsScheme();
        }
        if (!isAllowedS3ImageUri(uri, directory)) {
            throw InvalidImageUrlException.notAllowed();
        }

        return url;
    }

    public static List<String> allowedFileExtensions() {
        return ALLOWED_IMAGE_EXTENSIONS;
    }

    private URI parse(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw InvalidImageUrlException.invalidFormat();
        }
    }

    private boolean isAllowedS3ImageUri(URI uri, S3ImageDirectory directory) {
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return false;
        }
        if (uri.getHost() == null || !expectedHost().equalsIgnoreCase(uri.getHost())) {
            return false;
        }

        String path = uri.getPath();
        String pathPrefix = directory.uriPrefix();
        if (path == null || !path.startsWith(pathPrefix) || path.length() == pathPrefix.length()) {
            return false;
        }

        String extension = StringUtils.getFilenameExtension(path);
        return extension != null && ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private String expectedHost() {
        return "%s.s3.%s.amazonaws.com".formatted(s3Properties.getBucket(), s3Properties.getRegion());
    }
}
