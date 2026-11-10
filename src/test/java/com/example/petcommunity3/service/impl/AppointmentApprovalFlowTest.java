package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Appointment;
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

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentApprovalFlowTest {
    @Test
    void merchantApprovalLocksCapacityBeforeMarkingAppointmentApproved() {
        TestContext context = context();
        Appointment appointment = pendingAppointment();
        when(context.appointmentMapper.selectById(11L)).thenReturn(appointment);
        executeSlotAction(context.slotLockService);

        context.service.updateStatus(2L, 11L, status(1, "通过预约"));

        verify(context.permissionService).requireStoreOwnerOrAdmin(2L, 9L);
        verify(context.slotLockService).executeWithLock(eq("schedule:8"), any());
        verify(context.scheduleService).lockSlot(8L, 9L, 4L, appointment.getAppointmentTime());
        assertThat(appointment.getStatus()).isEqualTo(1);
        verify(context.appointmentMapper).updateById(appointment);
    }

    @Test
    void fullScheduleLeavesAppointmentPending() {
        TestContext context = context();
        Appointment appointment = pendingAppointment();
        when(context.appointmentMapper.selectById(11L)).thenReturn(appointment);
        executeSlotAction(context.slotLockService);
        doThrow(new RuntimeException("该排班名额已满"))
                .when(context.scheduleService).lockSlot(8L, 9L, 4L, appointment.getAppointmentTime());

        assertThatThrownBy(() -> context.service.updateStatus(2L, 11L, status(1, "通过预约")))
                .hasMessage("该排班名额已满");

        assertThat(appointment.getStatus()).isEqualTo(3);
        verify(context.appointmentMapper, never()).updateById(any(Appointment.class));
    }

    @Test
    void userCanCancelPendingRequestWithoutReleasingCapacity() {
        TestContext context = context();
        Appointment appointment = pendingAppointment();
        appointment.setUserId(6L);
        when(context.permissionService.requireActiveUser(6L)).thenReturn(activeUser(6L));
        when(context.appointmentMapper.selectOne(any())).thenReturn(appointment);

        context.service.cancel(6L, 11L, 6L);

        assertThat(appointment.getStatus()).isEqualTo(0);
        verify(context.scheduleService, never()).releaseSlot(any());
    }

    @Test
    void merchantRejectsPendingRequestWithoutReleasingCapacity() {
        TestContext context = context();
        Appointment appointment = pendingAppointment();
        when(context.appointmentMapper.selectById(11L)).thenReturn(appointment);

        context.service.updateStatus(2L, 11L, status(0, "无法接待"));

        assertThat(appointment.getStatus()).isEqualTo(0);
        verify(context.scheduleService, never()).releaseSlot(any());
    }

    private TestContext context() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        ServiceScheduleService scheduleService = mock(ServiceScheduleService.class);
        AppointmentSlotLockService slotLockService = mock(AppointmentSlotLockService.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl(
                appointmentMapper,
                mock(UserMapper.class),
                mock(PetMapper.class),
                mock(StoreMapper.class),
                mock(ServiceItemMapper.class),
                mock(NotificationService.class),
                scheduleService,
                slotLockService,
                permissionService
        );
        return new TestContext(service, appointmentMapper, scheduleService, slotLockService, permissionService);
    }

    private void executeSlotAction(AppointmentSlotLockService slotLockService) {
        when(slotLockService.executeWithLock(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Callable<Void> action = invocation.getArgument(1);
            return action.call();
        });
    }

    private Appointment pendingAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(11L);
        appointment.setUserId(6L);
        appointment.setStoreId(9L);
        appointment.setServiceItemId(4L);
        appointment.setServiceScheduleId(8L);
        appointment.setAppointmentTime(LocalDateTime.of(2026, 11, 12, 10, 0));
        appointment.setStatus(3);
        return appointment;
    }

    private StatusUpdateDTO status(Integer status, String remark) {
        StatusUpdateDTO dto = new StatusUpdateDTO();
        dto.setStatus(status);
        dto.setRemark(remark);
        return dto;
    }

    private com.example.petcommunity3.entity.User activeUser(Long id) {
        com.example.petcommunity3.entity.User user = new com.example.petcommunity3.entity.User();
        user.setId(id);
        user.setStatus(1);
        return user;
    }

    private record TestContext(AppointmentServiceImpl service,
                               AppointmentMapper appointmentMapper,
                               ServiceScheduleService scheduleService,
                               AppointmentSlotLockService slotLockService,
                               MerchantPermissionService permissionService) {
    }
}
