package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.OrderCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.vo.OrderDetailVO;

import java.util.List;

public interface ProductOrderService {
    // 创建订单：从当前用户购物车生成订单，返回新订单 id。
    Long create(OrderCreateDTO dto);

    // 查询订单列表：根据用户 id 返回该用户的订单。
    List<ProductOrder> listByUserId(Long userId);

    // 查询订单详情：返回订单主表信息和订单明细。
    OrderDetailVO getDetail(Long id, Long userId);

    // 取消订单：只允许用户取消自己的待处理订单。
    void cancel(Long id, Long userId);

    // 完成订单：后台或商户端把订单改成已完成。
    void finish(Long id);

    // 通用状态更新：后台统一修改订单状态，并写入通知。
    void updateStatus(Long id, StatusUpdateDTO dto);
}
