package com.dduru.gildongmu.post.share.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.share")
public class PostShareProperties {

    private String baseUrl = "https://api.dduru.app";
    private String androidPackageName = "com.gildongmu.dduru";
    private List<String> androidCertFingerprints = new ArrayList<>();
    private String playStoreUrl = "";

    public List<String> normalizedAndroidCertFingerprints() {
        if (androidCertFingerprints == null) {
            return List.of();
        }

        return androidCertFingerprints.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    public String postShareUrl(Long postId) {
        return baseUrl.replaceAll("/+$", "") + "/share/posts/" + postId;
    }

    public boolean hasPlayStoreUrl() {
        return playStoreUrl != null && !playStoreUrl.isBlank();
    }
}
