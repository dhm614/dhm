package com.example.petcommunity3.controller;

import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.ImageUpdateDTO;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.service.ProductService;
import com.example.petcommunity3.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    // 商品接口入口：负责商品新增、列表、详情、修改、下架和封面更新。
    private final ProductService productService;
    private final AdminService adminService;

    // 注入商品业务层，库存、价格、状态等校验放在 Service 中。
    public ProductController(ProductService productService, AdminService adminService) {
        this.productService = productService;
        this.adminService = adminService;
    }

    // POST /api/products
    // 新增商品，请求体包含商品名称、分类、价格、库存等信息。
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ProductCreateDTO dto) {
        adminService.requireAdmin(operatorId);
        productService.create(dto);
        return Result.success(null);
    }

    // GET /api/products?category=FOOD
    // 查询上架商品列表，category 可选，不传则查全部上架商品。
    @GetMapping
    public Result<List<Product>> list(@RequestParam(required = false) String category) {
        List<Product> products = productService.list(category);
        return Result.success(products);
    }

    // GET /api/products/{id}
    // 查询单个商品详情。
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        Product product = productService.getDetail(id);
        return Result.success(product);
    }

    // PUT /api/products
    // 修改商品信息。
    @PutMapping
    public Result<Void> update(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                               @RequestBody ProductUpdateDTO dto) {
        adminService.requireAdmin(operatorId);
        productService.update(dto);
        return Result.success(null);
    }

    // DELETE /api/products/{id}
    // 下架商品，不物理删除，避免影响历史订单。
    @DeleteMapping("/{id}")
    public Result<Void> disable(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                @PathVariable Long id) {
        adminService.requireAdmin(operatorId);
        productService.disable(id);
        return Result.success(null);
    }

    // PUT /api/products/{id}/cover
    // 文件上传后，把图片 URL 写入商品封面字段。
    @PutMapping("/{id}/cover")
    public Result<Void> updateCover(@RequestHeader(value = "X-User-Id", required = false) Long operatorId,
                                    @PathVariable Long id,
                                    @RequestBody ImageUpdateDTO dto) {
        adminService.requireAdmin(operatorId);
        productService.updateCover(id, dto.getUrl());
        return Result.success(null);
    }
}
