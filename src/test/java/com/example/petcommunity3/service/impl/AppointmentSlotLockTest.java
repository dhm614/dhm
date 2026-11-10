package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.AppointmentCreateDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentSlotLockTest {

    @Test
    void createAppointmentStoresPendingRequestWithoutLockingSchedule() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PetMapper petMapper = mock(PetMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        ServiceItemMapper serviceItemMapper = mock(ServiceItemMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        ServiceScheduleService scheduleService = mock(ServiceScheduleService.class);
        AppointmentSlotLockService slotLockService = mock(AppointmentSlotLockService.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl(
                appointmentMapper,
                userMapper,
                petMapper,
                storeMapper,
                serviceItemMapper,
                notificationService,
                scheduleService,
                slotLockService,
                permissionService
        );
        AppointmentCreateDTO dto = dto();

        when(userMapper.selectById(1L)).thenReturn(user());
        when(permissionService.requireActiveUser(1L)).thenReturn(user());
        when(petMapper.selectById(2L)).thenReturn(pet());
        when(storeMapper.selectById(3L)).thenReturn(store());
        when(serviceItemMapper.selectById(4L)).thenReturn(serviceItem());
        when(slotLockService.executeWithLock(eq("schedule:8"), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Callable<Void> action = invocation.getArgument(1);
            return action.call();
        });

        service.create(1L, dto);

        org.mockito.ArgumentCaptor<Appointment> captor = org.mockito.ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(3);
        verify(slotLockService, never()).executeWithLock(any(), any());
        verify(scheduleService, never()).lockSlot(any(), any(), any(), any());
    }

    @Test
    void createAppointmentRequiresScheduleForCapacityApproval() {
        AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PetMapper petMapper = mock(PetMapper.class);
        StoreMapper storeMapper = mock(StoreMapper.class);
        ServiceItemMapper serviceItemMapper = mock(ServiceItemMapper.class);
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl(
                appointmentMapper,
                userMapper,
                petMapper,
                storeMapper,
                serviceItemMapper,
                mock(NotificationService.class),
                mock(ServiceScheduleService.class),
                mock(AppointmentSlotLockService.class),
                permissionService
        );
        AppointmentCreateDTO dto = dto();
        dto.setServiceScheduleId(null);
        when(permissionService.requireActiveUser(1L)).thenReturn(user());
        when(userMapper.selectById(1L)).thenReturn(user());
        when(petMapper.selectById(2L)).thenReturn(pet());
        when(storeMapper.selectById(3L)).thenReturn(store());
        when(serviceItemMapper.selectById(4L)).thenReturn(serviceItem());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(1L, dto))
                .hasMessage("请选择服务排班");

        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    private AppointmentCreateDTO dto() {
        AppointmentCreateDTO dto = new AppointmentCreateDTO();
        dto.setUserId(1L);
        dto.setPetId(2L);
        dto.setStoreId(3L);
        dto.setServiceItemId(4L);
        dto.setServiceScheduleId(8L);
        dto.setAppointmentTime(LocalDateTime.of(2026, 8, 28, 10, 0));
        return dto;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        return user;
    }

    private Pet pet() {
        Pet pet = new Pet();
        pet.setId(2L);
        pet.setUserId(1L);
        return pet;
    }

    private Store store() {
        Store store = new Store();
        store.setId(3L);
        store.setStatus(1);
        return store;
    }

    private ServiceItem serviceItem() {
        ServiceItem item = new ServiceItem();
        item.setId(4L);
        item.setStoreId(3L);
        item.setStatus(1);
        item.setPrice(BigDecimal.TEN);
        return item;
    }
}
