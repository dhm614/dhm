package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.CartAddDTO;
import com.example.petcommunity3.dto.CartUpdateDTO;
import com.example.petcommunity3.entity.CartItem;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.CartItemMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.CartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    // CartServiceImpl 负责购物车模块：添加商品、查询购物车、修改数量、删除和清空。
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public CartServiceImpl(CartItemMapper cartItemMapper,
                           ProductMapper productMapper,
                           UserMapper userMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void add(CartAddDTO dto) {
        // 添加购物车：校验用户和商品，如果已存在同一商品就累加数量。
        checkUser(dto.getUserId());

        if (dto.getProductId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }

        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        Product product = getNormalProduct(dto.getProductId());

        CartItem existItem = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, dto.getUserId())
                        .eq(CartItem::getProductId, dto.getProductId())
        );

        if (existItem != null) {
            int newQuantity = existItem.getQuantity() + quantity;
            if (newQuantity > product.getStock()) {
                throw new RuntimeException("商品库存不足");
            }

            existItem.setQuantity(newQuantity);
            cartItemMapper.updateById(existItem);
            return;
        }

        if (quantity > product.getStock()) {
            throw new RuntimeException("商品库存不足");
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(dto.getUserId());
        cartItem.setProductId(dto.getProductId());
        cartItem.setQuantity(quantity);

        cartItemMapper.insert(cartItem);
    }

    @Override
    public List<CartItem> listByUserId(Long userId) {
        // 查询用户购物车：按 userId 找出当前用户的购物车记录。
        checkUser(userId);

        return cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getCreateTime)
        );
    }

    @Override
    public void updateQuantity(CartUpdateDTO dto) {
        // 修改购物车数量：先确认记录属于该用户，再检查库存是否足够。
        if (dto.getId() == null) {
            throw new RuntimeException("购物车ID不能为空");
        }

        checkUser(dto.getUserId());

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        CartItem cartItem = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getId, dto.getId())
                        .eq(CartItem::getUserId, dto.getUserId())
        );

        if (cartItem == null) {
            throw new RuntimeException("购物车商品不存在");
        }

        Product product = getNormalProduct(cartItem.getProductId());
        if (dto.getQuantity() > product.getStock()) {
            throw new RuntimeException("商品库存不足");
        }

        cartItem.setQuantity(dto.getQuantity());
        cartItemMapper.updateById(cartItem);
    }

    @Override
    public void delete(Long id, Long userId) {
        // 删除购物车项：用 id + userId 防止删除别人的购物车记录。
        if (id == null) {
            throw new RuntimeException("购物车ID不能为空");
        }

        checkUser(userId);

        CartItem cartItem = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getId, id)
                        .eq(CartItem::getUserId, userId)
        );

        if (cartItem == null) {
            throw new RuntimeException("购物车商品不存在");
        }

        cartItemMapper.deleteById(id);
    }

    @Override
    public void clear(Long userId) {
        // 清空购物车：删除该用户的全部购物车项。
        checkUser(userId);

        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
        );
    }

    private void checkUser(Long userId) {
        // 公共用户校验：购物车操作必须绑定有效用户。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
    }

    private Product getNormalProduct(Long productId) {
        // 公共商品校验：只能加入正常上架的商品。
        Product product = productMapper.selectById(productId);

        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new RuntimeException("商品不存在或已下架");
        }

        return product;
    }
}
