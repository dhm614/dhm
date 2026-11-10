package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.StoreCreateDTO;
import com.example.petcommunity3.dto.StoreUpdateDTO;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.service.StoreService;
import com.example.petcommunity3.vo.StoreDistanceVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {
    // 门店接口入口：提供门店新增、查询、修改、停用和附近门店查询。
    private final StoreService storeService;

    // 注入门店业务层，Controller 不直接写门店查询或距离计算逻辑。
    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // POST /api/stores
    // 新增门店，通常给后台管理员或商户端使用。
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody StoreCreateDTO dto) {
        storeService.create(operatorId, dto);
        return Result.success(null);
    }

    // GET /api/stores
    // 查询正常营业的门店列表。
    @GetMapping
    public Result<List<Store>> list() {
        List<Store> stores = storeService.list();
        return Result.success(stores);
    }

    // GET /api/stores/{id}
    // 查询单个门店详情，id 来自 URL 路径。
    @GetMapping("/{id}")
    public Result<Store> detail(@PathVariable Long id) {
        Store store = storeService.getDetail(id);
        return Result.success(store);
    }

    // PUT /api/stores
    // 修改门店资料，Service 会先确认门店是否存在。
    @PutMapping
    public Result<Void> update(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody StoreUpdateDTO dto) {
        storeService.update(operatorId, dto);
        return Result.success(null);
    }

    // DELETE /api/stores/{id}
    // 这里不是物理删除，而是把门店 status 改成停用。
    @DeleteMapping("/{id}")
    public Result<Void> disable(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                @PathVariable Long id) {
        storeService.disable(operatorId, id);
        return Result.success(null);
    }

    // GET /api/stores/nearby?longitude=116.48&latitude=39.92
    // 根据用户当前位置计算附近门店，limit 可选，用来限制返回数量。
    @GetMapping("/nearby")
    public Result<List<StoreDistanceVO>> nearby(@RequestParam BigDecimal longitude,
                                                @RequestParam BigDecimal latitude,
                                                @RequestParam(required = false) Integer limit) {
        return Result.success(storeService.listNearby(longitude, latitude, limit));
    }
}
