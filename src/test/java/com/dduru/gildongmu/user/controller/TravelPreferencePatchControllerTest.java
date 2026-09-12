package com.dduru.gildongmu.user.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.exception.GlobalExceptionHandler;
import com.dduru.gildongmu.post.service.PostQueryService;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferencePatchRequest;
import com.dduru.gildongmu.recommendation.service.TravelPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TravelPreferencePatchControllerTest {
    private TravelPreferenceService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(TravelPreferenceService.class);
        mvc = standaloneSetup(new UserController(mock(PostQueryService.class), service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUser.class);
                    }
                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) {
                        return 10L;
                    }
                }).build();
    }

    @Test
    void destinationsOnlyPreservesOmittedDatesAndRequestOrder() throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPreferences":[{"type":"CITY","destinationId":2},{"type":"CITY","destinationId":1}]}
                                """))
                .andExpect(status().isNoContent());
        var captor = ArgumentCaptor.forClass(TravelPreferencePatchRequest.class);
        verify(service).patchTravelPreferences(eq(10L), captor.capture());
        assertThat(captor.getValue().getAvailableDates()).isNull();
        assertThat(captor.getValue().getDestinationPreferences()).extracting("destinationId").containsExactly(2L, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"destinationPreferences\":[]}", "{\"availableDates\":[]}",
            "{\"availableDates\":[{\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-03\"}]}"})
    void acceptsOmissionEmptyListsAndDates(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"destinationPreferences\":null}", "{\"availableDates\":null}",
            "{\"destinationPreferences\":[null]}", "{\"destinationPreferences\":[{}]}",
            "{\"availableDates\":[null]}", "{\"availableDates\":[{}]}",
            "{\"destinationPreferences\":[{\"type\":\"COUNTRY\"},{\"type\":\"COUNTRY\"},{\"type\":\"COUNTRY\"},{\"type\":\"COUNTRY\"}]}"})
    void invalidPatchReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void putStillRequiresBothLists() throws Exception {
        mvc.perform(put("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationPreferences\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
