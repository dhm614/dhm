package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOperationApplication;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOperationApplicationMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.ProductOperationApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductOperationApplicationServiceImpl implements ProductOperationApplicationService {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ProductOperationApplicationMapper applicationMapper;
    private final ProductMapper productMapper;
    private final MerchantPermissionService merchantPermissionService;

    public ProductOperationApplicationServiceImpl(ProductOperationApplicationMapper applicationMapper,
                                                  ProductMapper productMapper,
                                                  MerchantPermissionService merchantPermissionService) {
        this.applicationMapper = applicationMapper;
        this.productMapper = productMapper;
        this.merchantPermissionService = merchantPermissionService;
    }

    @Override
    public void submitCreate(Long merchantUserId, ProductCreateDTO dto) {
        merchantPermissionService.requireMerchantOrAdmin(merchantUserId);
        validateProduct(dto.getName(), dto.getCategory(), dto.getPrice(), dto.getStock());
        ProductOperationApplication application = new ProductOperationApplication();
        application.setMerchantUserId(merchantUserId);
        application.setOperationType("CREATE");
        copySnapshot(application, dto.getName(), dto.getCategory(), dto.getPrice(), dto.getStock(), dto.getCover(), dto.getDescription());
        application.setStatus(STATUS_PENDING);
        applicationMapper.insert(application);
    }

    @Override
    public void submitUpdate(Long merchantUserId, Long productId, ProductUpdateDTO dto) {
        merchantPermissionService.requireMerchantOrAdmin(merchantUserId);
        Product product = getOwnedProduct(merchantUserId, productId);
        ensureNoPendingApplication(productId);

        String name = StringUtils.hasText(dto.getName()) ? dto.getName() : product.getName();
        String category = StringUtils.hasText(dto.getCategory()) ? dto.getCategory() : product.getCategory();
        java.math.BigDecimal price = dto.getPrice() == null ? product.getPrice() : dto.getPrice();
        Integer stock = dto.getStock() == null ? product.getStock() : dto.getStock();
        String cover = dto.getCover() == null ? product.getCover() : dto.getCover();
        String description = dto.getDescription() == null ? product.getDescription() : dto.getDescription();
        validateProduct(name, category, price, stock);

        ProductOperationApplication application = new ProductOperationApplication();
        application.setMerchantUserId(merchantUserId);
        application.setProductId(productId);
        application.setOperationType("UPDATE");
        copySnapshot(application, name, category, price, stock, cover, description);
        application.setStatus(STATUS_PENDING);
        applicationMapper.insert(application);
    }

    @Override
    public void submitEnable(Long merchantUserId, Long productId) {
        submitStatusChange(merchantUserId, productId, "ENABLE");
    }

    @Override
    public void submitDisable(Long merchantUserId, Long productId) {
        submitStatusChange(merchantUserId, productId, "DISABLE");
    }

    @Override
    public List<Product> listMerchantProducts(Long merchantUserId) {
        merchantPermissionService.requireMerchantOrAdmin(merchantUserId);
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getMerchantUserId, merchantUserId)
                        .orderByDesc(Product::getCreateTime)
        );
    }

    @Override
    public List<ProductOperationApplication> listMerchantApplications(Long merchantUserId) {
        merchantPermissionService.requireMerchantOrAdmin(merchantUserId);
        return applicationMapper.selectList(
                new LambdaQueryWrapper<ProductOperationApplication>()
                        .eq(ProductOperationApplication::getMerchantUserId, merchantUserId)
                        .orderByDesc(ProductOperationApplication::getCreateTime)
        );
    }

    @Override
    public List<ProductOperationApplication> listAdminApplications(Integer status) {
        LambdaQueryWrapper<ProductOperationApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOperationApplication::getStatus, status == null ? STATUS_PENDING : status)
                .orderByDesc(ProductOperationApplication::getCreateTime);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void review(Long applicationId, Long reviewerId, ApplicationReviewDTO dto) {
        if (applicationId == null) {
            throw new RuntimeException("申请ID不能为空");
        }
        if (reviewerId == null) {
            throw new RuntimeException("审核人ID不能为空");
        }
        if (dto == null || dto.getStatus() == null
                || (!Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus())
                && !Integer.valueOf(STATUS_REJECTED).equals(dto.getStatus()))) {
            throw new RuntimeException("审核状态只能是1或2");
        }
        ProductOperationApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("商品操作申请不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(application.getStatus())) {
            throw new RuntimeException("申请已处理");
        }

        if (Integer.valueOf(STATUS_APPROVED).equals(dto.getStatus())) {
            applyApprovedOperation(application);
        }
        application.setStatus(dto.getStatus());
        application.setReviewerId(reviewerId);
        application.setReviewRemark(dto.getRemark());
        application.setReviewTime(LocalDateTime.now());
        applicationMapper.updateById(application);
    }

    private void submitStatusChange(Long merchantUserId, Long productId, String operationType) {
        merchantPermissionService.requireMerchantOrAdmin(merchantUserId);
        Product product = getOwnedProduct(merchantUserId, productId);
        ensureNoPendingApplication(productId);
        ProductOperationApplication application = new ProductOperationApplication();
        application.setMerchantUserId(merchantUserId);
        application.setProductId(productId);
        application.setOperationType(operationType);
        copySnapshot(application, product.getName(), product.getCategory(), product.getPrice(), product.getStock(),
                product.getCover(), product.getDescription());
        application.setStatus(STATUS_PENDING);
        applicationMapper.insert(application);
    }

    private void applyApprovedOperation(ProductOperationApplication application) {
        switch (application.getOperationType()) {
            case "CREATE" -> {
                Product product = new Product();
                product.setMerchantUserId(application.getMerchantUserId());
                applySnapshot(product, application);
                product.setSales(0);
                product.setStatus(1);
                productMapper.insert(product);
                application.setProductId(product.getId());
            }
            case "UPDATE" -> {
                Product product = getApplicationProduct(application);
                applySnapshot(product, application);
                productMapper.updateById(product);
            }
            case "ENABLE" -> {
                Product product = getApplicationProduct(application);
                product.setStatus(1);
                productMapper.updateById(product);
            }
            case "DISABLE" -> {
                Product product = getApplicationProduct(application);
                product.setStatus(0);
                productMapper.updateById(product);
            }
            default -> throw new RuntimeException("不支持的商品操作类型");
        }
    }

    private Product getApplicationProduct(ProductOperationApplication application) {
        Product product = productMapper.selectById(application.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!application.getMerchantUserId().equals(product.getMerchantUserId())) {
            throw new RuntimeException("无权操作该商户数据");
        }
        return product;
    }

    private Product getOwnedProduct(Long merchantUserId, Long productId) {
        if (productId == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!merchantUserId.equals(product.getMerchantUserId())) {
            throw new RuntimeException("无权操作该商户数据");
        }
        return product;
    }

    private void ensureNoPendingApplication(Long productId) {
        Long count = applicationMapper.selectCount(
                new LambdaQueryWrapper<ProductOperationApplication>()
                        .eq(ProductOperationApplication::getProductId, productId)
                        .eq(ProductOperationApplication::getStatus, STATUS_PENDING)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("已有待审核申请");
        }
    }

    private void validateProduct(String name, String category, java.math.BigDecimal price, Integer stock) {
        if (!StringUtils.hasText(name)) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (!StringUtils.hasText(category)) {
            throw new RuntimeException("商品分类不能为空");
        }
        if (price == null || price.signum() < 0) {
            throw new RuntimeException("商品价格不合法");
        }
        if (stock == null || stock < 0) {
            throw new RuntimeException("商品库存不合法");
        }
    }

    private void copySnapshot(ProductOperationApplication application,
                              String name,
                              String category,
                              java.math.BigDecimal price,
                              Integer stock,
                              String cover,
                              String description) {
        application.setProductName(name);
        application.setCategory(category);
        application.setPrice(price);
        application.setStock(stock);
        application.setCover(cover);
        application.setDescription(description);
    }

    private void applySnapshot(Product product, ProductOperationApplication application) {
        product.setName(application.getProductName());
        product.setCategory(application.getCategory());
        product.setPrice(application.getPrice());
        product.setStock(application.getStock());
        product.setCover(application.getCover());
        product.setDescription(application.getDescription());
    }
}
