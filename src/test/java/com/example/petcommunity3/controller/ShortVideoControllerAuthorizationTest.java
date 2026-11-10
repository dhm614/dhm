package com.example.petcommunity3.controller;

import com.example.petcommunity3.dto.ShortVideoCreateDTO;
import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.ShortVideoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ShortVideoControllerAuthorizationTest {
    @Test
    void createVideoRejectsMissingHeaderBeforeServiceAccess() throws Exception {
        ShortVideoService service = mock(ShortVideoService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(post("/api/videos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(99L)))
                .andExpect(jsonPath("$.code").value(500));

        verify(service, never()).create(any());
    }

    @Test
    void createVideoUsesHeaderIdentityInsteadOfBodyIdentity() throws Exception {
        ShortVideoService service = mock(ShortVideoService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(post("/api/videos")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(99L)))
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<ShortVideoCreateDTO> captor = ArgumentCaptor.forClass(ShortVideoCreateDTO.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    }

    private MockMvc mockMvc(ShortVideoService service) {
        return MockMvcBuilders.standaloneSetup(new ShortVideoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String validBody(Long userId) {
        return "{\"userId\":" + userId
                + ",\"title\":\"video\",\"content\":\"content\""
                + ",\"cover\":\"/uploads/short-video-cover/demo.jpg\""
                + ",\"videoUrl\":\"/uploads/short-video/demo.mp4\"}";
    }
}
