package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.CartAddDTO;
import com.example.petcommunity3.dto.CartUpdateDTO;
import com.example.petcommunity3.entity.CartItem;
import com.example.petcommunity3.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart-items")
public class CartController {
    // 购物车接口入口：添加商品、查看购物车、修改数量、删除或清空购物车。
    private final CartService cartService;

    // 注入购物车业务层，商品库存校验和重复商品合并逻辑都在 Service 中。
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // POST /api/cart-items
    // 添加商品到购物车，请求体包含 userId、productId、quantity。
    @PostMapping
    public Result<Void> add(@RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.success(null);
    }

    // GET /api/cart-items?userId=1
    // 查询某个用户的购物车列表。
    @GetMapping
    public Result<List<CartItem>> list(@RequestParam Long userId) {
        List<CartItem> cartItems = cartService.listByUserId(userId);
        return Result.success(cartItems);
    }

    // PUT /api/cart-items
    // 修改购物车中某个商品的购买数量。
    @PutMapping
    public Result<Void> updateQuantity(@RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(dto);
        return Result.success(null);
    }

    // DELETE /api/cart-items/clear?userId=1
    // 清空某个用户的整个购物车。
    @DeleteMapping("/clear")
    public Result<Void> clear(@RequestParam Long userId) {
        cartService.clear(userId);
        return Result.success(null);
    }

    // DELETE /api/cart-items/{id}?userId=1
    // 删除购物车中的单条记录，同时校验这条记录属于该用户。
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        cartService.delete(id, userId);
        return Result.success(null);
    }
}
