package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ServiceItemCreateDTO;
import com.example.petcommunity3.dto.ServiceItemUpdateDTO;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.service.ServiceItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-items")
public class ServiceItemController {
    // 服务项目接口入口：管理门店下的洗护、寄养、诊疗等服务。
    private final ServiceItemService serviceItemService;

    // 注入服务项目业务层，门店校验和上下架逻辑都交给 Service。
    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    // POST /api/service-items
    // 新增服务项目，请求体包含 storeId、名称、分类、价格等信息。
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ServiceItemCreateDTO dto) {
        serviceItemService.create(operatorId, dto);
        return Result.success(null);
    }

    // GET /api/service-items?storeId=1
    // 查询某个门店下所有上架服务项目。
    @GetMapping
    public Result<List<ServiceItem>> listByStoreId(@RequestParam Long storeId) {
        List<ServiceItem> items = serviceItemService.listByStoreId(storeId);
        return Result.success(items);
    }

    // GET /api/service-items/{id}
    // 查询单个服务项目详情。
    @GetMapping("/{id}")
    public Result<ServiceItem> detail(@PathVariable Long id) {
        ServiceItem item = serviceItemService.getDetail(id);
        return Result.success(item);
    }

    // PUT /api/service-items
    // 修改服务项目资料，例如价格、时长、描述、状态。
    @PutMapping
    public Result<Void> update(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ServiceItemUpdateDTO dto) {
        serviceItemService.update(operatorId, dto);
        return Result.success(null);
    }

    // DELETE /api/service-items/{id}
    // 下架服务项目，不直接删除历史数据。
    @DeleteMapping("/{id}")
    public Result<Void> disable(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                @PathVariable Long id) {
        serviceItemService.disable(operatorId, id);
        return Result.success(null);
    }
}
