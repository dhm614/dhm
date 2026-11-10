package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ServiceScheduleCreateDTO;
import com.example.petcommunity3.dto.ServiceScheduleUpdateDTO;
import com.example.petcommunity3.entity.ServiceSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceScheduleService {
    // 新增排班：商户为某个门店服务配置可预约时间和容量。
    void create(Long operatorId, ServiceScheduleCreateDTO dto);

    // 查询排班：支持按门店、服务项目、日期筛选。
    List<ServiceSchedule> list(Long storeId, Long serviceItemId, LocalDate scheduleDate);

    // 查询可预约排班：只返回启用且还有剩余名额的时间槽。
    List<ServiceSchedule> listAvailable(Long storeId, Long serviceItemId, LocalDate scheduleDate);

    // 修改排班：调整时间、容量、状态等排班信息。
    void update(Long operatorId, ServiceScheduleUpdateDTO dto);

    // 停用排班：不再允许用户选择这个时间槽。
    void disable(Long operatorId, Long id);

    // 查询排班详情：预约或后台管理时按 ID 获取完整信息。
    ServiceSchedule getDetail(Long id);

    // 校验预约选择的排班、门店、服务和时间是否匹配，但不占用名额。
    void validateSlot(Long scheduleId, Long storeId, Long serviceItemId, LocalDateTime appointmentTime);

    // 锁定时间槽：商户通过预约时占用一个名额，防止超约。
    void lockSlot(Long scheduleId, Long storeId, Long serviceItemId, LocalDateTime appointmentTime);

    // 释放时间槽：取消预约时把名额还回去。
    void releaseSlot(Long scheduleId);
}
