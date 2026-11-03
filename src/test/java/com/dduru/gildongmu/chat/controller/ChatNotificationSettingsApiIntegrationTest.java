package com.dduru.gildongmu.chat.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("채팅방 알림 API 인증 및 OpenAPI 통합 테스트")
class ChatNotificationSettingsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void securityFilterRequiresAuthenticationForBothEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/1/notification-settings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/chat-rooms/1/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiContainsSettingsEndpointsAndSchemas() throws Exception {
        String path = "$.paths['/api/v1/chat-rooms/{chatRoomId}/notification-settings']";
        mockMvc.perform(get("/v3/api-docs/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path + ".get.responses['200']").exists())
                .andExpect(jsonPath(path + ".get.responses['403']").exists())
                .andExpect(jsonPath(path + ".get.responses['404']").exists())
                .andExpect(jsonPath(path + ".patch.responses['200']").exists())
                .andExpect(jsonPath(path + ".patch.responses['400']").exists())
                .andExpect(jsonPath(path + ".patch.responses['401']").exists())
                .andExpect(jsonPath("$.components.schemas.ChatNotificationSettingsRequest.required[0]").value("enabled"))
                .andExpect(jsonPath("$.components.schemas.ChatNotificationSettingsResponse.properties.enabled.type").value("boolean"));
    }
}
