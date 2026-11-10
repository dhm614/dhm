package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.PostService;
import com.example.petcommunity3.service.ShortVideoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MyContentControllerTest {
    @Test
    void myPostsUsesCurrentUserHeader() throws Exception {
        PostService service = mock(PostService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/posts/my").header("X-User-Id", "7"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).listByUserId(7L);
    }

    @Test
    void myPostsRejectsMissingCurrentUserHeader() throws Exception {
        PostService service = mock(PostService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/posts/my"))
                .andExpect(jsonPath("$.code").value(500));

        verify(service, never()).listByUserId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void myVideosUsesCurrentUserHeader() throws Exception {
        ShortVideoService service = mock(ShortVideoService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ShortVideoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/videos/my").header("X-User-Id", "8"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).listByUserId(8L);
    }

    @Test
    void myVideosRejectsMissingCurrentUserHeader() throws Exception {
        ShortVideoService service = mock(ShortVideoService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ShortVideoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/videos/my"))
                .andExpect(jsonPath("$.code").value(500));

        verify(service, never()).listByUserId(org.mockito.ArgumentMatchers.anyLong());
    }
}
