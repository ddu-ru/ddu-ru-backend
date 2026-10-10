package com.dduru.gildongmu.notification.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.exception.GlobalExceptionHandler;
import com.dduru.gildongmu.notification.domain.enums.NotificationFilter;
import com.dduru.gildongmu.notification.dto.request.NotificationListRequest;
import com.dduru.gildongmu.notification.dto.response.NotificationListResponse;
import com.dduru.gildongmu.notification.dto.response.UnreadCountResponse;
import com.dduru.gildongmu.notification.service.NotificationQueryService;
import com.dduru.gildongmu.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    @Mock
    private NotificationQueryService notificationQueryService;
    @Mock
    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new NotificationController(notificationQueryService, notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUser.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) {
                        return 1L;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("쿼리 파라미터 미전달 시 ALL 필터로 조회한다")
    void missingFilterDefaultsToAll() throws Exception {
        NotificationListRequest request = new NotificationListRequest(null, null, NotificationFilter.ALL);
        when(notificationQueryService.getNotifications(1L, request))
                .thenReturn(NotificationListResponse.of(List.of(), 20));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications").isEmpty());

        verify(notificationQueryService).getNotifications(1L, request);
    }

    @Test
    @DisplayName("필터, 커서, 크기를 요청 DTO로 바인딩한다")
    void bindsFilterCursorAndSize() throws Exception {
        NotificationListRequest request = new NotificationListRequest(30L, 10, NotificationFilter.JOURNEY);
        when(notificationQueryService.getNotifications(1L, request))
                .thenReturn(NotificationListResponse.of(List.of(), 10));

        mockMvc.perform(get("/api/v1/notifications").param("filter", "JOURNEY")
                        .param("cursor", "30").param("size", "10"))
                .andExpect(status().isOk());

        verify(notificationQueryService).getNotifications(1L, request);
    }

    @ParameterizedTest
    @CsvSource({"filter, MESSAGE", "size, 0", "size, 51", "cursor, 0"})
    @DisplayName("지원하지 않는 필터나 잘못된 페이지 파라미터는 400이다")
    void invalidQueryReturnsBadRequest(String name, String value) throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param(name, value))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationQueryService);
    }

    @Test
    @DisplayName("미읽음 응답은 기존 전체 개수와 탭별 개수를 함께 제공한다")
    void returnsCategoryUnreadCounts() throws Exception {
        when(notificationQueryService.getUnreadCount(1L)).thenReturn(new UnreadCountResponse(3, 2, 1));

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andExpect(jsonPath("$.data.matchUnreadCount").value(2))
                .andExpect(jsonPath("$.data.journeyUnreadCount").value(1));
    }

    @Test
    @DisplayName("전체 삭제는 현재 사용자 기준으로 실행하고 본문 없는 204를 반환한다")
    void deleteAllReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(notificationService).deleteAllNotifications(1L);
    }
}
