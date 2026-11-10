package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.AppointmentSlotLockService;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.ServiceScheduleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentAuthorizationTest {

    @Test
    void ordinaryUserCannotReadAnotherUsersAppointments() {
        TestContext context = context();
        when(context.permissionService.requireActiveUser(2L)).thenReturn(user(2L, "USER"));

        assertThatThrownBy(() -> context.service.listByUserId(2L, 8L))
                .hasMessage("无权查看其他用户预约");

        verify(context.appointmentMapper, never()).selectList(any());
    }

    @Test
    void administratorCanReadAnotherUsersAppointments() {
        TestContext context = context();
        when(context.permissionService.requireActiveUser(3L)).thenReturn(user(3L, "ADMIN"));
        when(context.appointmentMapper.selectList(any())).thenReturn(List.of());

        assertThat(context.service.listByUserId(3L, 8L)).isEmpty();
    }

    @Test
    void merchantCannotReadAppointmentsForAnotherMerchantsStore() {
        TestContext context = context();
        doThrow(new RuntimeException("无权操作该商户数据"))
                .when(context.permissionService).requireStoreOwnerOrAdmin(2L, 9L);

        assertThatThrownBy(() -> context.service.listByStoreId(2L, 9L))
                .hasMessage("无权操作该商户数据");

        verify(context.appointmentMapper, never()).selectList(any());
    }

    @Test
    void merchantCanFinishAppointmentForOwnedStore() {
        TestContext context = context();
        Appointment appointment = appointment(11L, 9L, 1);
        when(context.appointmentMapper.selectById(11L)).thenReturn(appointment);

        context.service.finish(2L, 11L);

        verify(context.permissionService).requireStoreOwnerOrAdmin(2L, 9L);
        assertThat(appointment.getStatus()).isEqualTo(2);
        verify(context.appointmentMapper).updateById(appointment);
    }

    @Test
    void statusNotificationUsesAuthenticatedOperatorId() {
        TestContext context = context();
        Appointment appointment = appointment(11L, 9L, 1);
        appointment.setUserId(6L);
        when(context.appointmentMapper.selectById(11L)).thenReturn(appointment);
        StatusUpdateDTO dto = new StatusUpdateDTO();
        dto.setStatus(2);
        dto.setOperatorId(99L);

        context.service.updateStatus(2L, 11L, dto);

        verify(context.notificationService).create(
                org.mockito.ArgumentMatchers.eq(6L),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq("APPOINTMENT_STATUS"),
                any(),
                org.mockito.ArgumentMatchers.eq("APPOINTMENT"),
                org.mockito.ArgumentMatchers.eq(11L));
    }

    private TestContext context() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl(
                appointmentMapper,
                mock(UserMapper.class),
                mock(PetMapper.class),
                mock(StoreMapper.class),
                mock(ServiceItemMapper.class),
                notificationService,
                mock(ServiceScheduleService.class),
                mock(AppointmentSlotLockService.class),
                permissionService
        );
        return new TestContext(service, appointmentMapper, notificationService, permissionService);
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private Appointment appointment(Long id, Long storeId, Integer status) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStoreId(storeId);
        appointment.setStatus(status);
        return appointment;
    }

    private record TestContext(AppointmentServiceImpl service,
                               AppointmentMapper appointmentMapper,
                               NotificationService notificationService,
                               MerchantPermissionService permissionService) {
    }
}
