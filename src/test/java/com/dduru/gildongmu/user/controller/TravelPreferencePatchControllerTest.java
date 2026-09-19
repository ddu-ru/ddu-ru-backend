package com.dduru.gildongmu.user.controller;

import com.dduru.gildongmu.common.annotation.CurrentUser;
import com.dduru.gildongmu.common.exception.GlobalExceptionHandler;
import com.dduru.gildongmu.post.service.PostQueryService;
import com.dduru.gildongmu.recommendation.dto.request.TravelPreferenceUpdateRequest;
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
        var captor = ArgumentCaptor.forClass(TravelPreferenceUpdateRequest.class);
        verify(service).updateTravelPreferences(eq(10L), captor.capture());
        assertThat(captor.getValue().availableDates()).isNull();
        assertThat(captor.getValue().destinationPreferences()).extracting("destinationId").containsExactly(2L, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"destinationPreferences\":[]}", "{\"availableDates\":[]}",
            "{\"availableDates\":[{\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-03\"}]}"})
    void acceptsOmissionEmptyListsAndDates(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"destinationPreferences\":null}", "{\"availableDates\":null}",
            "{\"destinationPreferences\":null,\"availableDates\":null}"})
    void omissionAndExplicitNullLeaveBothListsUnchanged(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        var captor = ArgumentCaptor.forClass(TravelPreferenceUpdateRequest.class);
        verify(service).updateTravelPreferences(eq(10L), captor.capture());
        assertThat(captor.getValue().destinationPreferences()).isNull();
        assertThat(captor.getValue().availableDates()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"destinationPreferences\":[],\"availableDates\":null}",
            "{\"destinationPreferences\":null,\"availableDates\":[]}"})
    void explicitNullPreservesOneListWhileEmptyArrayClearsTheOther(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        var captor = ArgumentCaptor.forClass(TravelPreferenceUpdateRequest.class);
        verify(service).updateTravelPreferences(eq(10L), captor.capture());
        if (body.contains("\"destinationPreferences\":[]")) {
            assertThat(captor.getValue().destinationPreferences()).isEmpty();
            assertThat(captor.getValue().availableDates()).isNull();
        } else {
            assertThat(captor.getValue().destinationPreferences()).isNull();
            assertThat(captor.getValue().availableDates()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"destinationPreferences\":[null]}", "{\"destinationPreferences\":[{}]}",
            "{\"availableDates\":[null]}", "{\"availableDates\":[{}]}"})
    void invalidPatchReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void moreThanThreeDestinationsReturns400BeforeDeduplication() throws Exception {
        mvc.perform(patch("/api/v1/users/me/travel-preferences").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationPreferences":[
                                  {"type":"COUNTRY","countryCode":"JP"},
                                  {"type":"COUNTRY","countryCode":"JP"},
                                  {"type":"COUNTRY","countryCode":"JP"},
                                  {"type":"COUNTRY","countryCode":"JP"}
                                ]}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

}
