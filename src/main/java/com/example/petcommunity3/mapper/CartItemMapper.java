package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}