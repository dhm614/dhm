package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.AdminService;
import com.example.petcommunity3.service.ContentReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ContentReportControllerAuthorizationTest {
    @Test
    void reportListRejectsMissingAndOrdinaryUserIdentityBeforeServiceAccess() throws Exception {
        ContentReportService reportService = mock(ContentReportService.class);
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("admin required")).when(adminService).requireAdmin(null);
        doThrow(new RuntimeException("admin required")).when(adminService).requireAdmin(2L);
        MockMvc mvc = mockMvc(reportService, adminService);

        mvc.perform(get("/api/reports")).andExpect(jsonPath("$.code").value(500));
        mvc.perform(get("/api/reports").header("X-User-Id", "2"))
                .andExpect(jsonPath("$.code").value(500));

        verify(reportService, never()).list(any());
    }

    @Test
    void reportHandleRejectsMissingAndOrdinaryUserIdentityBeforeServiceAccess() throws Exception {
        ContentReportService reportService = mock(ContentReportService.class);
        AdminService adminService = mock(AdminService.class);
        doThrow(new RuntimeException("admin required")).when(adminService).requireAdmin(null);
        doThrow(new RuntimeException("admin required")).when(adminService).requireAdmin(2L);
        MockMvc mvc = mockMvc(reportService, adminService);
        String body = "{\"status\":1,\"handleResult\":\"confirmed\"}";

        mvc.perform(put("/api/reports/9/handle").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(500));
        mvc.perform(put("/api/reports/9/handle").header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(500));

        verify(reportService, never()).handle(anyLong(), any());
    }

    @Test
    void reportListAndHandleAllowAdminIdentity() throws Exception {
        ContentReportService reportService = mock(ContentReportService.class);
        AdminService adminService = mock(AdminService.class);
        MockMvc mvc = mockMvc(reportService, adminService);

        mvc.perform(get("/api/reports").header("X-User-Id", "3").param("status", "0"))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/api/reports/9/handle").header("X-User-Id", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1,\"handleResult\":\"confirmed\"}"))
                .andExpect(jsonPath("$.code").value(200));

        verify(adminService, org.mockito.Mockito.times(2)).requireAdmin(3L);
        verify(reportService).list(0);
        verify(reportService).handle(org.mockito.ArgumentMatchers.eq(9L), any());
    }

    private MockMvc mockMvc(ContentReportService reportService, AdminService adminService) {
        return MockMvcBuilders.standaloneSetup(new ContentReportController(reportService, adminService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
