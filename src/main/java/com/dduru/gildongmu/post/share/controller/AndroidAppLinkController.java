package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.post.share.config.PostShareProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AndroidAppLinkController {

    private static final String HANDLE_ALL_URLS = "delegate_permission/common.handle_all_urls";

    private final PostShareProperties postShareProperties;

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AssetLinkStatement> retrieveAssetLinks() {
        List<String> fingerprints = postShareProperties.normalizedAndroidCertFingerprints();
        if (fingerprints.isEmpty()) {
            return List.of();
        }

        AssetLinkTarget target = new AssetLinkTarget(
                "android_app",
                postShareProperties.getAndroidPackageName(),
                fingerprints
        );
        return List.of(new AssetLinkStatement(List.of(HANDLE_ALL_URLS), target));
    }

    public record AssetLinkStatement(
            List<String> relation,
            AssetLinkTarget target
    ) {
    }

    public record AssetLinkTarget(
            String namespace,
            @JsonProperty("package_name") String packageName,
            @JsonProperty("sha256_cert_fingerprints") List<String> sha256CertFingerprints
    ) {
    }
}
