package com.dduru.gildongmu.post.share.controller;

import com.dduru.gildongmu.post.share.config.PostShareProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@DisplayName("AndroidAppLinkController 테스트")
class AndroidAppLinkControllerTest {

    @Test
    @DisplayName("인증서 지문이 없으면 빈 배열을 반환한다")
    void returnsEmptyArrayWithoutFingerprint() throws Exception {
        PostShareProperties properties = new PostShareProperties();
        MockMvc mockMvc = mockMvc(properties);

        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("설정된 복수 인증서 지문을 Digital Asset Links 형식으로 반환한다")
    void returnsAssetLinkStatement() throws Exception {
        PostShareProperties properties = new PostShareProperties();
        properties.setAndroidPackageName("com.gildongmu.dduru");
        properties.setAndroidCertFingerprints(List.of(" AA:BB ", "CC:DD", "AA:BB"));
        MockMvc mockMvc = mockMvc(properties);

        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relation[0]").value("delegate_permission/common.handle_all_urls"))
                .andExpect(jsonPath("$[0].target.namespace").value("android_app"))
                .andExpect(jsonPath("$[0].target.package_name").value("com.gildongmu.dduru"))
                .andExpect(jsonPath("$[0].target.sha256_cert_fingerprints[0]").value("AA:BB"))
                .andExpect(jsonPath("$[0].target.sha256_cert_fingerprints[1]").value("CC:DD"));
    }

    private MockMvc mockMvc(PostShareProperties properties) {
        return standaloneSetup(new AndroidAppLinkController(properties)).build();
    }
}
