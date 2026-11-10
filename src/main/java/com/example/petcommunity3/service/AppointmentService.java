package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.AppointmentCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Appointment;

import java.util.List;

public interface AppointmentService {
    // 创建预约申请：校验用户、宠物、门店、服务项目和排班时间后，以待商户确认状态插入。
    void create(Long operatorId, AppointmentCreateDTO dto);

    // 查询用户预约：给前台“我的预约”页面使用。
    List<Appointment> listByUserId(Long operatorId, Long userId);

    // 查询门店预约：给商户端查看某个门店收到的预约。
    List<Appointment> listByStoreId(Long operatorId, Long storeId);

    // 取消预约：待确认申请直接取消；已通过预约取消时释放排班名额。
    void cancel(Long operatorId, Long id, Long userId);

    // 完成预约：商户或后台把预约状态改为已完成。
    void finish(Long operatorId, Long id);

    // 通用状态更新：后台或商户端统一修改预约状态，并生成通知。
    void updateStatus(Long operatorId, Long id, StatusUpdateDTO dto);
}
