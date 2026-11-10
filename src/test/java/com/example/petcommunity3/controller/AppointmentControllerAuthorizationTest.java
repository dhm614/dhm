package com.example.petcommunity3.controller;

import com.example.petcommunity3.exception.GlobalExceptionHandler;
import com.example.petcommunity3.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AppointmentControllerAuthorizationTest {

    @Test
    void createPassesAuthenticatedUserHeaderToService() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(post("/api/appointments")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"petId\":2,\"storeId\":3,\"serviceItemId\":4,\"appointmentTime\":\"2026-11-12T10:00:00\"}"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).create(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void userAppointmentListPassesAuthenticatedUserHeaderToService() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(get("/api/appointments/user")
                        .header("X-User-Id", "7")
                        .param("userId", "7"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).listByUserId(7L, 7L);
    }

    @Test
    void storeAppointmentListPassesAuthenticatedUserHeaderToService() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(get("/api/appointments/store")
                        .header("X-User-Id", "7")
                        .param("storeId", "9"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).listByStoreId(7L, 9L);
    }

    @Test
    void statusUpdateUsesAuthenticatedUserInsteadOfBodyOperatorId() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        MockMvc mvc = mockMvc(service);

        mvc.perform(put("/api/appointments/11/status")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2,\"operatorId\":99,\"remark\":\"服务完成\"}"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).updateStatus(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(11L), any());
    }

    private MockMvc mockMvc(AppointmentService service) {
        return MockMvcBuilders.standaloneSetup(new AppointmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
