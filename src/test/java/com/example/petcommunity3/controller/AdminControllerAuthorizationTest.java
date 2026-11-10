package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminControllerAuthorizationTest {

    @Test
    void adminUserListRejectsRequestWithoutAdminHeader() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(adminService).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/admin/users"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先登录管理员账号"));

        verify(adminService).requireAdmin(null);
        verify(adminService, never()).listUsers(any());
    }

    @Test
    void adminVideoListRejectsRequestWithoutAdminHeaderBeforeServiceAction() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(adminService).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/admin/audit/videos"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先登录管理员账号"));

        verify(adminService).requireAdmin(null);
        verify(adminService, never()).listVideos(any(), any());
    }

    @Test
    void adminPostDetailRejectsRequestWithoutAdminHeaderBeforeServiceAction() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(adminService).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/admin/audit/posts/7"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先登录管理员账号"));

        verify(adminService).requireAdmin(null);
        verify(adminService, never()).getPostDetail(7L);
    }

    @Test
    void adminVideoDetailRejectsRequestWithoutAdminHeaderBeforeServiceAction() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(adminService).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/admin/audit/videos/8"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先登录管理员账号"));

        verify(adminService).requireAdmin(null);
        verify(adminService, never()).getVideoDetail(8L);
    }

    @Test
    void adminTopicCreateRejectsRequestWithoutAdminHeader() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(adminService).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/admin/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试话题\",\"sort\":10}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请先登录管理员账号"));

        verify(adminService).requireAdmin(null);
        verify(adminService, never()).createTopic(any());
    }

    @Test
    void adminTopicCreateRejectsNormalUserHeaderBeforeServiceAction() throws Exception {
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("管理员权限不足")).when(adminService).requireAdmin(2L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/admin/topics")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试话题\",\"sort\":10}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("管理员权限不足"));

        verify(adminService).requireAdmin(2L);
        verify(adminService, never()).createTopic(any());
    }
}
