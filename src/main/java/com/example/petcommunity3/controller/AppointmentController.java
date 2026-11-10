package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.AppointmentCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.service.AppointmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    // 预约接口入口：前台用户创建/取消预约，商户或后台完成/更新预约状态。
    private final AppointmentService appointmentService;

    // 注入预约业务层，复杂校验如用户、宠物、门店、排班占用都在 Service 中处理。
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // POST /api/appointments
    // 创建预约，请求体 JSON 会转成 AppointmentCreateDTO。
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody AppointmentCreateDTO dto) {
        appointmentService.create(operatorId, dto);
        return Result.success(null);
    }

    // GET /api/appointments/user?userId=1
    // 查询某个用户自己的预约记录。
    @GetMapping("/user")
    public Result<List<Appointment>> listByUserId(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
            @RequestParam Long userId) {
        List<Appointment> appointments = appointmentService.listByUserId(operatorId, userId);
        return Result.success(appointments);
    }

    // GET /api/appointments/store?storeId=1
    // 查询某个门店收到的预约记录，适合商户端管理预约。
    @GetMapping("/store")
    public Result<List<Appointment>> listByStoreId(
            @RequestHeader(value = "X-User-Id", required = false) Long operatorId,
            @RequestParam Long storeId) {
        List<Appointment> appointments = appointmentService.listByStoreId(operatorId, storeId);
        return Result.success(appointments);
    }

    // PUT /api/appointments/{id}/cancel?userId=1
    // 用户取消自己的预约，id 来自路径，userId 用来校验预约归属。
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @PathVariable Long id,
                               @RequestParam Long userId) {
        appointmentService.cancel(operatorId, id, userId);
        return Result.success(null);
    }

    // PUT /api/appointments/{id}/finish
    // 商户或后台把预约标记为已完成。
    @PutMapping("/{id}/finish")
    public Result<Void> finish(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @PathVariable Long id) {
        appointmentService.finish(operatorId, id);
        return Result.success(null);
    }

    // PUT /api/appointments/{id}/status
    // 通用状态更新接口，适合后台或商户端统一处理取消、完成等状态变化。
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                     @PathVariable Long id,
                                     @RequestBody StatusUpdateDTO dto) {
        appointmentService.updateStatus(operatorId, id, dto);
        return Result.success(null);
    }
}
