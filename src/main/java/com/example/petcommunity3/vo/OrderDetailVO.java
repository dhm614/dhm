package com.example.petcommunity3.vo;

import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.ProductOrderItem;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailVO {
    private ProductOrder order;
    private List<ProductOrderItem> items;
}