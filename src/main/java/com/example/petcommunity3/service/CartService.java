package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.CartAddDTO;
import com.example.petcommunity3.dto.CartUpdateDTO;
import com.example.petcommunity3.entity.CartItem;

import java.util.List;

public interface CartService {
    // 添加购物车：如果同一用户已有同一商品，则累加数量。
    void add(CartAddDTO dto);

    // 查询购物车：根据用户 id 查询购物车条目。
    List<CartItem> listByUserId(Long userId);

    // 修改数量：更新购物车某一项的购买数量。
    void updateQuantity(CartUpdateDTO dto);

    // 删除单项：根据购物车 id 和用户 id 删除，防止误删别人的购物车项。
    void delete(Long id, Long userId);

    // 清空购物车：删除某个用户的所有购物车项。
    void clear(Long userId);
}
