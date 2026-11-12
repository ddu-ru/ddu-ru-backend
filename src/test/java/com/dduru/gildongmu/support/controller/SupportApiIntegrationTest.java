package com.dduru.gildongmu.support.controller;

import com.dduru.gildongmu.admin.support.dto.request.InquiryAnswerRequest;
import com.dduru.gildongmu.admin.support.service.AdminInquiryService;
import com.dduru.gildongmu.support.domain.enums.*;
import com.dduru.gildongmu.support.dto.request.*;
import com.dduru.gildongmu.support.dto.response.*;
import com.dduru.gildongmu.support.service.*;
import com.dduru.gildongmu.support.exception.InquiryAlreadyAnsweredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SupportApiIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean FaqService faqs;
    @MockitoBean InquiryService inquiries;
    @MockitoBean AdminInquiryService admin;
    @BeforeEach
    void useNumericPrincipalAsJwtFilterDoes() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (authentication != null) {
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    authentication.getName(), null, authentication.getAuthorities()));
        }
    }

    private static final String VALID = "{\"category\":\"BUG\",\"content\":\"　문의　\",\"imageUrls\":[]}";

    @Test
    void guestsCannotAccessSupportApis() throws Exception {
        for (String path : List.of("/faqs", "/inquiries", "/inquiries/1", "/admin/inquiries", "/admin/inquiries/1")) {
            mvc.perform(get("/api/v1" + path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/inquiries").contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/inquiries/1").contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/inquiries/1/answer").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답변\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void ordinaryUsersCannotAccessAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/inquiries")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inquiries/1")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/inquiries/1/answer").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"답변\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(admin);
    }

    @Test
    @WithMockUser(username = "1")
    void authenticatedUserCanSearchAndCreateWithNormalizedContent() throws Exception {
        when(faqs.list(any())).thenReturn(List.of());
        when(inquiries.list(anyLong(), any())).thenReturn(InquiryListResponse.of(List.of(), 20));
        mvc.perform(get("/api/v1/faqs").param("keyword", "　사진　").param("category", "SERVICE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(faqs).list(new FaqListRequest(FaqCategory.SERVICE, "사진"));
        mvc.perform(get("/api/v1/inquiries")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inquiries").isEmpty()).andExpect(jsonPath("$.data.hasNext").value(false));
        mvc.perform(post("/api/v1/inquiries").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201));
        verify(inquiries).create(1L, new InquiryWriteRequest(InquiryCategory.BUG, "문의", List.of()));
    }

    @ParameterizedTest
    @WithMockUser(username = "1")
    @ValueSource(strings = {"{}", "{\"category\":\"BUG\",\"content\":\"　\"}",
            "{\"category\":\"UNKNOWN\",\"content\":\"내용\"}",
            "{\"category\":\"BUG\",\"content\":\"내용\",\"imageUrls\":[\"a\",\"b\",\"c\",\"d\",\"e\"]}",
            "{\"category\":\"BUG\",\"content\":\"내용\",\"imageUrls\":[null]}"})
    void invalidWritesReturn400(String body) throws Exception {
        mvc.perform(post("/api/v1/inquiries").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/inquiries/1").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(inquiries);
    }

    @Test
    @WithMockUser(username = "1")
    void invalidPageAndLengthAreRejected() throws Exception {
        for (String query : List.of("size=0", "size=51", "cursor=0", "size=abc")) {
            mvc.perform(get("/api/v1/inquiries?" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/faqs?category=UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/inquiries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"BUG\",\"content\":\"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inquiries);
    }

    @Test
    @WithMockUser(username = "1")
    void answeredEditReturns409() throws Exception {
        when(inquiries.update(eq(1L), eq(1L), any())).thenThrow(new InquiryAlreadyAnsweredException());
        mvc.perform(put("/api/v1/inquiries/1").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.data.errorCode").value("INQUIRY_ALREADY_ANSWERED"));
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void adminCanAnswerAndInvalidAnswerIsRejected() throws Exception {
        mvc.perform(put("/api/v1/admin/inquiries/1/answer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" 답변 \"}")).andExpect(status().isOk());
        verify(admin).updateAnswer(1L, 2L, new InquiryAnswerRequest("답변"));
        mvc.perform(put("/api/v1/admin/inquiries/1/answer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocumentsContractsAndSeparatesAdminGroup() throws Exception {
        mvc.perform(get("/v3/api-docs/public")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/faqs'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inquiries'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inquiries/{id}'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/inquiries']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.InquiryWriteRequest.properties.content.maxLength").value(500))
                .andExpect(jsonPath("$.components.schemas.InquiryWriteRequest.properties.imageUrls.maxItems").value(4));
        mvc.perform(get("/v3/api-docs/admin")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/inquiries/{id}/answer'].put.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/inquiries']").doesNotExist());
    }
}
