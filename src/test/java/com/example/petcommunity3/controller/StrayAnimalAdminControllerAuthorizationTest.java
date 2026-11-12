package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class StrayAnimalAdminControllerAuthorizationTest {

    @Test
    void adminRoutesCheckAdministratorBeforeDomainService() throws Exception {
        AdminService admin = mock(AdminService.class);
        StrayAnimalReportService reports = mock(StrayAnimalReportService.class);
        StrayAnimalService animals = mock(StrayAnimalService.class);
        StrayAdoptionService adoptions = mock(StrayAdoptionService.class);
        doThrow(new RuntimeException("请先登录管理员账号")).when(admin).requireAdmin(null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new StrayAnimalAdminController(admin, reports, animals, adoptions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/api/admin/stray-animal-reports"))
                .andExpect(jsonPath("$.code").value(500));
        verify(admin).requireAdmin(null);
        verifyNoInteractions(reports, animals, adoptions);
    }
}
