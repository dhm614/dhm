package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.StrayAdoptionService;
import com.example.petcommunity3.service.StrayAnimalReportService;
import com.example.petcommunity3.service.StrayAnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class StrayAnimalControllerTest {

    @Test
    void nearbyIsPublicAndPassesOptionalViewer() throws Exception {
        StrayAnimalReportService reports = mock(StrayAnimalReportService.class);
        StrayAnimalService animals = mock(StrayAnimalService.class);
        StrayAdoptionService adoptions = mock(StrayAdoptionService.class);
        when(animals.listNearby(any(), any(), isNull(), isNull())).thenReturn(List.of());
        MockMvc mvc = mvc(reports, animals, adoptions);

        mvc.perform(get("/api/stray-animals/nearby")
                        .param("longitude", "118.904104")
                        .param("latitude", "31.908685"))
                .andExpect(jsonPath("$.code").value(200));

        verify(animals).listNearby(any(), any(), isNull(), isNull());
    }

    @Test
    void reportSubmissionLetsServiceRejectMissingUser() throws Exception {
        StrayAnimalReportService reports = mock(StrayAnimalReportService.class);
        StrayAnimalService animals = mock(StrayAnimalService.class);
        StrayAdoptionService adoptions = mock(StrayAdoptionService.class);
        doThrow(new RuntimeException("用户不存在或已被禁用")).when(reports).submit(isNull(), any());

        mvc(reports, animals, adoptions).perform(post("/api/stray-animal-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("用户不存在或已被禁用"));
    }

    private MockMvc mvc(StrayAnimalReportService reports,
                        StrayAnimalService animals,
                        StrayAdoptionService adoptions) {
        return MockMvcBuilders.standaloneSetup(new StrayAnimalController(reports, animals, adoptions))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
