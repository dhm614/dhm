package com.example.petcommunity3.service.impl;

import com.example.petcommunity3.dto.ApplicationReviewDTO;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.entity.ProductOperationApplication;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.mapper.ProductOperationApplicationMapper;
import com.example.petcommunity3.service.MerchantPermissionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductOperationApplicationFlowTest {

    @Test
    void createSubmissionOnlyCreatesPendingApplication() {
        ProductOperationApplicationMapper applicationMapper = mock(ProductOperationApplicationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductOperationApplicationServiceImpl service = service(applicationMapper, productMapper);

        service.submitCreate(2L, productCreate("猫粮"));

        ArgumentCaptor<ProductOperationApplication> captor = ArgumentCaptor.forClass(ProductOperationApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getOperationType()).isEqualTo("CREATE");
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void approvedCreateApplicationInsertsMerchantProduct() {
        ProductOperationApplicationMapper applicationMapper = mock(ProductOperationApplicationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductOperationApplicationServiceImpl service = service(applicationMapper, productMapper);
        ProductOperationApplication application = application("CREATE", null);
        when(applicationMapper.selectById(10L)).thenReturn(application);

        service.review(10L, 3L, approved());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        assertThat(captor.getValue().getMerchantUserId()).isEqualTo(2L);
        assertThat(captor.getValue().getName()).isEqualTo("猫粮");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(application.getStatus()).isEqualTo(1);
    }

    @Test
    void approvedUpdateApplicationAppliesSnapshot() {
        ProductOperationApplicationMapper applicationMapper = mock(ProductOperationApplicationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductOperationApplicationServiceImpl service = service(applicationMapper, productMapper);
        Product product = product(6L, 2L, 1);
        ProductOperationApplication application = application("UPDATE", 6L);
        application.setProductName("低敏猫粮");
        when(applicationMapper.selectById(11L)).thenReturn(application);
        when(productMapper.selectById(6L)).thenReturn(product);

        service.review(11L, 3L, approved());

        assertThat(product.getName()).isEqualTo("低敏猫粮");
        verify(productMapper).updateById(product);
    }

    @Test
    void approvedDisableApplicationChangesProductStatus() {
        ProductOperationApplicationMapper applicationMapper = mock(ProductOperationApplicationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductOperationApplicationServiceImpl service = service(applicationMapper, productMapper);
        Product product = product(6L, 2L, 1);
        when(applicationMapper.selectById(12L)).thenReturn(application("DISABLE", 6L));
        when(productMapper.selectById(6L)).thenReturn(product);

        service.review(12L, 3L, approved());

        assertThat(product.getStatus()).isEqualTo(0);
        verify(productMapper).updateById(product);
    }

    @Test
    void updateSubmissionStoresFullSnapshotWithoutChangingProduct() {
        ProductOperationApplicationMapper applicationMapper = mock(ProductOperationApplicationMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductOperationApplicationServiceImpl service = service(applicationMapper, productMapper);
        Product product = product(6L, 2L, 1);
        when(productMapper.selectById(6L)).thenReturn(product);
        when(applicationMapper.selectCount(any())).thenReturn(0L);
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName("新包装猫粮");
        dto.setCategory("主粮");
        dto.setPrice(new BigDecimal("88.00"));
        dto.setStock(20);

        service.submitUpdate(2L, 6L, dto);

        ArgumentCaptor<ProductOperationApplication> captor = ArgumentCaptor.forClass(ProductOperationApplication.class);
        verify(applicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getOperationType()).isEqualTo("UPDATE");
        assertThat(captor.getValue().getProductName()).isEqualTo("新包装猫粮");
        verify(productMapper, never()).updateById(any(Product.class));
    }

    private ProductOperationApplicationServiceImpl service(ProductOperationApplicationMapper applicationMapper,
                                                           ProductMapper productMapper) {
        MerchantPermissionService permissionService = mock(MerchantPermissionService.class);
        return new ProductOperationApplicationServiceImpl(applicationMapper, productMapper, permissionService);
    }

    private ProductCreateDTO productCreate(String name) {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName(name);
        dto.setCategory("主粮");
        dto.setPrice(new BigDecimal("69.00"));
        dto.setStock(30);
        dto.setDescription("测试商品");
        return dto;
    }

    private ProductOperationApplication application(String type, Long productId) {
        ProductOperationApplication application = new ProductOperationApplication();
        application.setId(10L);
        application.setMerchantUserId(2L);
        application.setProductId(productId);
        application.setOperationType(type);
        application.setProductName("猫粮");
        application.setCategory("主粮");
        application.setPrice(new BigDecimal("69.00"));
        application.setStock(30);
        application.setStatus(0);
        return application;
    }

    private Product product(Long id, Long merchantId, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setMerchantUserId(merchantId);
        product.setName("猫粮");
        product.setCategory("主粮");
        product.setPrice(new BigDecimal("69.00"));
        product.setStock(30);
        product.setStatus(status);
        return product;
    }

    private ApplicationReviewDTO approved() {
        ApplicationReviewDTO dto = new ApplicationReviewDTO();
        dto.setStatus(1);
        dto.setRemark("审核通过");
        return dto;
    }
}
