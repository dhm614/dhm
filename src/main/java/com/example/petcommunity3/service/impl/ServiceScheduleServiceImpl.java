package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ServiceScheduleCreateDTO;
import com.example.petcommunity3.dto.ServiceScheduleUpdateDTO;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.ServiceSchedule;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.ServiceScheduleMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.ServiceScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ServiceScheduleServiceImpl implements ServiceScheduleService {
    // ServiceScheduleServiceImpl 负责门店服务排班：配置可预约时间段，并维护已预约名额。
    private final ServiceScheduleMapper serviceScheduleMapper;
    private final StoreMapper storeMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final MerchantPermissionService merchantPermissionService;

    public ServiceScheduleServiceImpl(ServiceScheduleMapper serviceScheduleMapper,
                                      StoreMapper storeMapper,
                                      ServiceItemMapper serviceItemMapper,
                                      MerchantPermissionService merchantPermissionService) {
        // 构造方法注入排班、门店、服务项目 Mapper，方便做跨表校验。
        this.serviceScheduleMapper = serviceScheduleMapper;
        this.storeMapper = storeMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.merchantPermissionService = merchantPermissionService;
    }

    @Override
    @Transactional
    public void create(Long operatorId, ServiceScheduleCreateDTO dto) {
        // 新增排班：校验门店和服务项目，再确认同一时间段没有重叠排班。
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, dto.getStoreId());
        validateBase(dto.getStoreId(), dto.getServiceItemId(), dto.getScheduleDate(), dto.getStartTime(), dto.getEndTime());
        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new RuntimeException("可预约容量必须大于0");
        }
        ensureNoOverlap(null, dto.getStoreId(), dto.getServiceItemId(), dto.getScheduleDate(), dto.getStartTime(), dto.getEndTime());

        // DTO 转成 ServiceSchedule 实体；bookedCount 从 0 开始，status=1 表示启用。
        ServiceSchedule schedule = new ServiceSchedule();
        schedule.setStoreId(dto.getStoreId());
        schedule.setServiceItemId(dto.getServiceItemId());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setCapacity(dto.getCapacity());
        schedule.setBookedCount(0);
        schedule.setStatus(1);
        schedule.setRemark(dto.getRemark());
        serviceScheduleMapper.insert(schedule);
    }

    @Override
    public List<ServiceSchedule> list(Long storeId, Long serviceItemId, LocalDate scheduleDate) {
        // 动态查询排班：参数不为空才加入查询条件。
        LambdaQueryWrapper<ServiceSchedule> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(ServiceSchedule::getStoreId, storeId);
        }
        if (serviceItemId != null) {
            wrapper.eq(ServiceSchedule::getServiceItemId, serviceItemId);
        }
        if (scheduleDate != null) {
            wrapper.eq(ServiceSchedule::getScheduleDate, scheduleDate);
        }
        wrapper.orderByAsc(ServiceSchedule::getScheduleDate).orderByAsc(ServiceSchedule::getStartTime);
        return serviceScheduleMapper.selectList(wrapper);
    }

    @Override
    public List<ServiceSchedule> listAvailable(Long storeId, Long serviceItemId, LocalDate scheduleDate) {
        // 可预约列表：只查启用状态，并且 booked_count 小于 capacity 的时间槽。
        LambdaQueryWrapper<ServiceSchedule> wrapper = new LambdaQueryWrapper<ServiceSchedule>()
                .eq(ServiceSchedule::getStatus, 1)
                .apply("booked_count < capacity");
        if (storeId != null) {
            wrapper.eq(ServiceSchedule::getStoreId, storeId);
        }
        if (serviceItemId != null) {
            wrapper.eq(ServiceSchedule::getServiceItemId, serviceItemId);
        }
        if (scheduleDate != null) {
            wrapper.eq(ServiceSchedule::getScheduleDate, scheduleDate);
        }
        wrapper.orderByAsc(ServiceSchedule::getScheduleDate).orderByAsc(ServiceSchedule::getStartTime);
        return serviceScheduleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void update(Long operatorId, ServiceScheduleUpdateDTO dto) {
        // 修改排班：未传的新字段沿用旧值，再统一校验是否有效。
        if (dto.getId() == null) {
            throw new RuntimeException("排班ID不能为空");
        }

        ServiceSchedule schedule = serviceScheduleMapper.selectById(dto.getId());
        if (schedule == null) {
            throw new RuntimeException("排班不存在");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, schedule.getStoreId());

        // 这些三元表达式表示：前端没传就用原来的值，传了就用新值。
        Long storeId = dto.getStoreId() == null ? schedule.getStoreId() : dto.getStoreId();
        Long serviceItemId = dto.getServiceItemId() == null ? schedule.getServiceItemId() : dto.getServiceItemId();
        LocalDate scheduleDate = dto.getScheduleDate() == null ? schedule.getScheduleDate() : dto.getScheduleDate();
        LocalTime startTime = dto.getStartTime() == null ? schedule.getStartTime() : dto.getStartTime();
        LocalTime endTime = dto.getEndTime() == null ? schedule.getEndTime() : dto.getEndTime();

        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, storeId);
        validateBase(storeId, serviceItemId, scheduleDate, startTime, endTime);
        ensureNoOverlap(dto.getId(), storeId, serviceItemId, scheduleDate, startTime, endTime);

        schedule.setStoreId(storeId);
        schedule.setServiceItemId(serviceItemId);
        schedule.setScheduleDate(scheduleDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        if (dto.getCapacity() != null) {
            if (dto.getCapacity() <= 0) {
                throw new RuntimeException("可预约容量必须大于0");
            }
            if (schedule.getBookedCount() != null && dto.getCapacity() < schedule.getBookedCount()) {
                throw new RuntimeException("可预约容量不能小于已预约数量");
            }
            schedule.setCapacity(dto.getCapacity());
        }
        if (dto.getBookedCount() != null) {
            if (dto.getBookedCount() < 0 || dto.getBookedCount() > schedule.getCapacity()) {
                throw new RuntimeException("已预约数量不合法");
            }
            schedule.setBookedCount(dto.getBookedCount());
        }
        if (dto.getStatus() != null) {
            schedule.setStatus(dto.getStatus());
        }
        schedule.setRemark(dto.getRemark());
        serviceScheduleMapper.updateById(schedule);
    }

    @Override
    public void disable(Long operatorId, Long id) {
        // 停用排班：逻辑删除，把 status 改成 0。
        ServiceSchedule schedule = getDetail(id);
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, schedule.getStoreId());
        schedule.setStatus(0);
        serviceScheduleMapper.updateById(schedule);
    }

    @Override
    public ServiceSchedule getDetail(Long id) {
        // 按 ID 查询排班详情，找不到就抛异常交给全局异常处理。
        if (id == null) {
            throw new RuntimeException("排班ID不能为空");
        }
        ServiceSchedule schedule = serviceScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new RuntimeException("排班不存在");
        }
        return schedule;
    }

    @Override
    public void validateSlot(Long scheduleId, Long storeId, Long serviceItemId, LocalDateTime appointmentTime) {
        validateSlotDetail(scheduleId, storeId, serviceItemId, appointmentTime);
    }

    @Override
    @Transactional
    public void lockSlot(Long scheduleId, Long storeId, Long serviceItemId, LocalDateTime appointmentTime) {
        // 锁定时间槽：商户通过预约时调用，确认排班可用后把 bookedCount 加 1。
        ServiceSchedule schedule = validateSlotDetail(scheduleId, storeId, serviceItemId, appointmentTime);
        int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
        int capacity = schedule.getCapacity() == null ? 0 : schedule.getCapacity();
        if (bookedCount >= capacity) {
            throw new RuntimeException("该排班名额已满");
        }

        // lockSlot 是 Mapper 里的原子更新：只有 booked_count < capacity 时才会成功加 1。
        if (serviceScheduleMapper.lockSlot(scheduleId) == 0) {
            throw new RuntimeException("该排班名额已满");
        }
    }

    private ServiceSchedule validateSlotDetail(Long scheduleId, Long storeId, Long serviceItemId,
                                               LocalDateTime appointmentTime) {
        if (scheduleId == null) {
            throw new RuntimeException("请选择服务排班");
        }
        if (appointmentTime == null) {
            throw new RuntimeException("预约时间不能为空");
        }

        ServiceSchedule schedule = getDetail(scheduleId);
        if (!Integer.valueOf(1).equals(schedule.getStatus())) {
            throw new RuntimeException("该排班已停用");
        }
        if (!schedule.getStoreId().equals(storeId) || !schedule.getServiceItemId().equals(serviceItemId)) {
            throw new RuntimeException("排班与门店或服务项目不匹配");
        }
        if (!schedule.getScheduleDate().equals(appointmentTime.toLocalDate())) {
            throw new RuntimeException("排班日期与预约时间不匹配");
        }
        LocalTime time = appointmentTime.toLocalTime();
        if (time.isBefore(schedule.getStartTime()) || !time.isBefore(schedule.getEndTime())) {
            throw new RuntimeException("预约时间不在排班时段内");
        }
        return schedule;
    }

    @Override
    @Transactional
    public void releaseSlot(Long scheduleId) {
        // 释放时间槽：取消预约时调用，把 bookedCount 减 1，但不能减到负数。
        if (scheduleId == null) {
            return;
        }

        ServiceSchedule schedule = serviceScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            return;
        }

        int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
        schedule.setBookedCount(Math.max(bookedCount - 1, 0));
        serviceScheduleMapper.updateById(schedule);
    }

    private void validateBase(Long storeId, Long serviceItemId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime) {
        // 基础校验：排班必须属于真实启用的门店和服务项目，且开始时间早于结束时间。
        if (storeId == null) {
            throw new RuntimeException("门店ID不能为空");
        }
        if (serviceItemId == null) {
            throw new RuntimeException("服务项目ID不能为空");
        }
        if (scheduleDate == null) {
            throw new RuntimeException("排班日期不能为空");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new RuntimeException("排班时间范围不合法");
        }

        Store store = storeMapper.selectById(storeId);
        if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
            throw new RuntimeException("门店不存在或已停用");
        }
        ServiceItem item = serviceItemMapper.selectById(serviceItemId);
        if (item == null || !Integer.valueOf(1).equals(item.getStatus())) {
            throw new RuntimeException("服务项目不存在或已停用");
        }
        if (!storeId.equals(item.getStoreId())) {
            throw new RuntimeException("服务项目不属于该门店");
        }
    }

    // 时间重叠判断：新时间段的开始早于旧结束，且新结束晚于旧开始，就算冲突。
    private void ensureNoOverlap(Long excludeId, Long storeId, Long serviceItemId, LocalDate scheduleDate,
                                 LocalTime startTime, LocalTime endTime) {
        LambdaQueryWrapper<ServiceSchedule> wrapper = new LambdaQueryWrapper<ServiceSchedule>()
                .eq(ServiceSchedule::getStoreId, storeId)
                .eq(ServiceSchedule::getServiceItemId, serviceItemId)
                .eq(ServiceSchedule::getScheduleDate, scheduleDate)
                .eq(ServiceSchedule::getStatus, 1)
                .lt(ServiceSchedule::getStartTime, endTime)
                .gt(ServiceSchedule::getEndTime, startTime);
        if (excludeId != null) {
            wrapper.ne(ServiceSchedule::getId, excludeId);
        }
        if (serviceScheduleMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("排班时间与已有排班冲突");
        }
    }
}
