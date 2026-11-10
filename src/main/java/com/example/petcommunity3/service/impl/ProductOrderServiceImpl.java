package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.OrderCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.CartItem;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOrder;
import com.example.petcommunity3.entity.ProductOrderItem;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.CartItemMapper;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOrderItemMapper;
import com.example.petcommunity3.mapper.ProductOrderMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.ProductOrderService;
import com.example.petcommunity3.vo.OrderDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProductOrderServiceImpl implements ProductOrderService {
    // ProductOrderServiceImpl 负责商城订单模块：从购物车生成模拟订单，并维护库存和销量。
    private final ProductOrderMapper productOrderMapper;
    private final ProductOrderItemMapper productOrderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    // 订单状态变化后，通过通知服务提醒用户。
    private final NotificationService notificationService;

    public ProductOrderServiceImpl(ProductOrderMapper productOrderMapper,
                                   ProductOrderItemMapper productOrderItemMapper,
                                   CartItemMapper cartItemMapper,
                                   ProductMapper productMapper,
                                   UserMapper userMapper,
                                   NotificationService notificationService) {
        // 构造方法注入订单、订单明细、购物车、商品、用户等数据库操作对象。
        this.productOrderMapper = productOrderMapper;
        this.productOrderItemMapper = productOrderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Long create(OrderCreateDTO dto) {
        // 创建订单是一个事务：订单、明细、库存、购物车必须保持一致。
        // 先确认下单用户存在且没有被禁用。
        checkUser(dto.getUserId());

        // 从购物车表查出该用户当前要结算的商品。
        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, dto.getUserId())
        );
        if (cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空");
        }

        // 逐项校验库存并累计订单总金额。
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = getNormalProduct(cartItem.getProductId());
            if (cartItem.getQuantity() > product.getStock()) {
                throw new RuntimeException("商品库存不足：" + product.getName());
            }
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // 先保存订单主表：订单号、用户、总金额、状态。
        ProductOrder order = new ProductOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(dto.getUserId());
        order.setTotalAmount(totalAmount);
        order.setRemark(dto.getRemark());
        order.setStatus(1);
        productOrderMapper.insert(order);

        // 再把每个购物车商品转成订单明细，同时扣库存、加销量。
        for (CartItem cartItem : cartItems) {
            Product product = getNormalProduct(cartItem.getProductId());

            ProductOrderItem orderItem = new ProductOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            productOrderItemMapper.insert(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setSales((product.getSales() == null ? 0 : product.getSales()) + cartItem.getQuantity());
            productMapper.updateById(product);
        }

        // 订单生成后清空该用户购物车，避免重复结算。
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, dto.getUserId())
        );

        sendOrderNotification(order, null, "订单已创建：" + order.getOrderNo());
        return order.getId();
    }

    @Override
    public List<ProductOrder> listByUserId(Long userId) {
        // 用户端查看自己的订单列表。
        checkUser(userId);
        return productOrderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getUserId, userId)
                        .orderByDesc(ProductOrder::getCreateTime)
        );
    }

    @Override
    public OrderDetailVO getDetail(Long id, Long userId) {
        // 订单详情由订单主表和订单明细表组合成 VO 返回给前端。
        if (id == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        checkUser(userId);

        ProductOrder order = productOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getId, id)
                        .eq(ProductOrder::getUserId, userId)
        );
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        List<ProductOrderItem> items = productOrderItemMapper.selectList(
                new LambdaQueryWrapper<ProductOrderItem>()
                        .eq(ProductOrderItem::getOrderId, id)
        );

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    @Override
    @Transactional
    public void cancel(Long id, Long userId) {
        // 取消订单：只能取消自己的待处理订单，并且要恢复库存和销量。
        if (id == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        checkUser(userId);

        ProductOrder order = productOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getId, id)
                        .eq(ProductOrder::getUserId, userId)
        );
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new RuntimeException("当前订单无法取消");
        }

        order.setStatus(0);
        productOrderMapper.updateById(order);
        // 订单取消后，把订单明细里的数量加回商品库存。
        restoreStock(order.getId());
        sendOrderNotification(order, userId, "订单已取消：" + order.getOrderNo());
    }

    @Override
    @Transactional
    public void finish(Long id) {
        // 完成订单：通常由后台或商户把订单状态改为已完成。
        if (id == null) {
            throw new RuntimeException("订单ID不能为空");
        }

        ProductOrder order = productOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new RuntimeException("当前订单无法完成");
        }

        order.setStatus(2);
        productOrderMapper.updateById(order);
        sendOrderNotification(order, null, "订单已完成：" + order.getOrderNo());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, StatusUpdateDTO dto) {
        // 后台通用状态修改：根据状态变化决定恢复库存还是重新扣库存。
        if (id == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        if (dto == null || dto.getStatus() == null) {
            throw new RuntimeException("订单状态不能为空");
        }
        if (!isValidStatus(dto.getStatus())) {
            throw new RuntimeException("订单状态无效");
        }

        ProductOrder order = productOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 保存旧状态，用来判断这次状态变化对库存有没有影响。
        Integer oldStatus = order.getStatus();
        order.setStatus(dto.getStatus());
        productOrderMapper.updateById(order);
        if (!Integer.valueOf(0).equals(oldStatus) && Integer.valueOf(0).equals(dto.getStatus())) {
            // 订单取消后，把订单明细里的数量加回商品库存。
        restoreStock(order.getId());
        } else if (Integer.valueOf(0).equals(oldStatus) && !Integer.valueOf(0).equals(dto.getStatus())) {
            deductStock(order.getId());
        }

        String content = "订单状态已变更为：" + orderStatusName(dto.getStatus()) + "，订单号：" + order.getOrderNo();
        if (StringUtils.hasText(dto.getRemark())) {
            content = content + "，备注：" + dto.getRemark();
        }
        sendOrderNotification(order, dto.getOperatorId(), content);
    }

    private void checkUser(Long userId) {
        // 公共用户校验：订单相关操作都要求用户存在且账号可用。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
    }

    private Product getNormalProduct(Long productId) {
        // 公共商品校验：只能购买和扣减正常上架的商品。
        Product product = productMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new RuntimeException("商品不存在");
        }
        return product;
    }

    private void sendOrderNotification(ProductOrder order, Long fromUserId, String content) {
        // 订单创建、取消、完成或状态变化后统一发通知。
        notificationService.create(
                order.getUserId(),
                fromUserId,
                "ORDER_STATUS",
                content,
                "ORDER",
                order.getId()
        );
    }

    private void restoreStock(Long orderId) {
        // 恢复库存：按订单明细逐个商品加回库存、减掉销量。
        List<ProductOrderItem> items = productOrderItemMapper.selectList(
                new LambdaQueryWrapper<ProductOrderItem>()
                        .eq(ProductOrderItem::getOrderId, orderId)
        );
        for (ProductOrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                continue;
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            product.setStock((product.getStock() == null ? 0 : product.getStock()) + quantity);
            product.setSales(Math.max((product.getSales() == null ? 0 : product.getSales()) - quantity, 0));
            productMapper.updateById(product);
        }
    }

    private void deductStock(Long orderId) {
        // 重新扣库存：先整体检查库存够不够，再真正扣减，避免扣到一半失败。
        List<ProductOrderItem> items = productOrderItemMapper.selectList(
                new LambdaQueryWrapper<ProductOrderItem>()
                        .eq(ProductOrderItem::getOrderId, orderId)
        );
        for (ProductOrderItem item : items) {
            Product product = getNormalProduct(item.getProductId());
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if ((product.getStock() == null ? 0 : product.getStock()) < quantity) {
                throw new RuntimeException("商品库存不足：" + product.getName());
            }
        }

        for (ProductOrderItem item : items) {
            Product product = getNormalProduct(item.getProductId());
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            product.setStock((product.getStock() == null ? 0 : product.getStock()) - quantity);
            product.setSales((product.getSales() == null ? 0 : product.getSales()) + quantity);
            productMapper.updateById(product);
        }
    }

    private boolean isValidStatus(Integer status) {
        // 订单状态约定：0取消，1已下单，2已完成。
        return Integer.valueOf(0).equals(status)
                || Integer.valueOf(1).equals(status)
                || Integer.valueOf(2).equals(status);
    }

    private String orderStatusName(Integer status) {
        // 把数字状态转换成通知里能读懂的文字。
        if (Integer.valueOf(0).equals(status)) {
            return "已取消";
        }
        if (Integer.valueOf(1).equals(status)) {
            return "已下单";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已完成";
        }
        return "未知";
    }

    private String generateOrderNo() {
        // 生成模拟订单号：PO + 当前时间，满足演示项目里的唯一性需求。
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
