package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ServiceScheduleCreateDTO;
import com.example.petcommunity3.dto.ServiceScheduleUpdateDTO;
import com.example.petcommunity3.entity.ServiceSchedule;
import com.example.petcommunity3.service.ServiceScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service-schedules")
public class ServiceScheduleController {
    // 排班接口入口：前端通过这里管理门店某个服务项目的可预约时间。
    private final ServiceScheduleService serviceScheduleService;

    public ServiceScheduleController(ServiceScheduleService serviceScheduleService) {
        // 构造方法注入 Service，Controller 只接收请求，不直接操作数据库。
        this.serviceScheduleService = serviceScheduleService;
    }

    // POST /api/service-schedules：新增一条排班时间。
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ServiceScheduleCreateDTO dto) {
        serviceScheduleService.create(operatorId, dto);
        return Result.success(null);
    }

    // GET /api/service-schedules：按门店、服务项目、日期筛选排班；参数都不是必填。
    @GetMapping
    public Result<List<ServiceSchedule>> list(@RequestParam(required = false) Long storeId,
                                              @RequestParam(required = false) Long serviceItemId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        return Result.success(serviceScheduleService.list(storeId, serviceItemId, scheduleDate));
    }

    // GET /api/service-schedules/available：只查询还有余量、可以被预约的排班。
    @GetMapping("/available")
    public Result<List<ServiceSchedule>> listAvailable(@RequestParam(required = false) Long storeId,
                                                       @RequestParam(required = false) Long serviceItemId,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        return Result.success(serviceScheduleService.listAvailable(storeId, serviceItemId, scheduleDate));
    }

    // GET /api/service-schedules/{id}：根据排班 ID 查询详情。
    @GetMapping("/{id}")
    public Result<ServiceSchedule> detail(@PathVariable Long id) {
        return Result.success(serviceScheduleService.getDetail(id));
    }

    // PUT /api/service-schedules：修改排班信息。
    @PutMapping
    public Result<Void> update(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ServiceScheduleUpdateDTO dto) {
        serviceScheduleService.update(operatorId, dto);
        return Result.success(null);
    }

    // DELETE /api/service-schedules/{id}：这里是逻辑停用排班，不一定物理删除。
    @DeleteMapping("/{id}")
    public Result<Void> disable(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                @PathVariable Long id) {
        serviceScheduleService.disable(operatorId, id);
        return Result.success(null);
    }
}
