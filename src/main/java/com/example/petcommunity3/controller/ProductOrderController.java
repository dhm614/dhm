package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.OrderCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.service.ProductOrderService;
import com.example.petcommunity3.vo.OrderDetailVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-orders")
public class ProductOrderController {
    // 商品订单接口入口：从购物车创建模拟订单，并支持查询、取消、完成和状态更新。
    private final ProductOrderService productOrderService;

    // 注入订单业务层，创建订单时的库存扣减、订单明细生成、购物车清空都在 Service 中。
    public ProductOrderController(ProductOrderService productOrderService) {
        this.productOrderService = productOrderService;
    }

    // POST /api/product-orders
    // 从当前用户购物车生成订单，返回新订单 id。
    @PostMapping
    public Result<Long> create(@RequestBody OrderCreateDTO dto) {
        Long orderId = productOrderService.create(dto);
        return Result.success(orderId);
    }

    // GET /api/product-orders?userId=1
    // 查询某个用户的订单列表。
    @GetMapping
    public Result<List<ProductOrder>> listByUserId(@RequestParam Long userId) {
        List<ProductOrder> orders = productOrderService.listByUserId(userId);
        return Result.success(orders);
    }

    // GET /api/product-orders/{id}?userId=1
    // 查询订单详情，返回订单主表和订单明细列表。
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id, @RequestParam Long userId) {
        OrderDetailVO detail = productOrderService.getDetail(id, userId);
        return Result.success(detail);
    }

    // PUT /api/product-orders/{id}/cancel?userId=1
    // 用户取消自己的订单。
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam Long userId) {
        productOrderService.cancel(id, userId);
        return Result.success(null);
    }

    // PUT /api/product-orders/{id}/finish
    // 后台或商户端把订单标记为已完成。
    @PutMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        productOrderService.finish(id);
        return Result.success(null);
    }

    // PUT /api/product-orders/{id}/status
    // 通用订单状态修改接口，会触发订单状态通知。
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateDTO dto) {
        productOrderService.updateStatus(id, dto);
        return Result.success(null);
    }
}
