package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;
import com.example.petcommunity3.mapper.ProductMapper;
import com.example.petcommunity3.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    // ProductServiceImpl 负责商城商品模块：商品创建、列表查询、详情、修改、下架和封面更新。
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public void create(ProductCreateDTO dto) {
        // 创建商品：校验名称、分类、价格、库存，再保存为 Product 实体。
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCategory())) {
            throw new RuntimeException("商品分类不能为空");
        }
        if (dto.getPrice() == null) {
            throw new RuntimeException("商品价格不能为空");
        }
        if (dto.getStock() == null) {
            throw new RuntimeException("商品库存不能为空");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCover(dto.getCover());
        product.setDescription(dto.getDescription());
        product.setSales(0);
        product.setStatus(1);
        productMapper.insert(product);
    }

    @Override
    public List<Product> list(String category) {
        // 商品列表：只展示上架商品；如果传了分类，就按分类筛选。
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (StringUtils.hasText(category)) {
            wrapper.eq(Product::getCategory, category);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectList(wrapper);
    }

    @Override
    public Product getDetail(Long id) {
        // 商品详情：根据 ID 查询，并确认商品存在且未下架。
        if (id == null) {
            throw new RuntimeException("商品ID不能为空");
        }

        Product product = productMapper.selectById(id);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new RuntimeException("商品不存在");
        }
        return product;
    }

    @Override
    public void update(ProductUpdateDTO dto) {
        // 修改商品：先查原商品，再覆盖前端传来的新字段。
        if (dto.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }

        Product product = productMapper.selectById(dto.getId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCover(dto.getCover());
        product.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }
        productMapper.updateById(product);
    }

    @Override
    public void disable(Long id) {
        // 下架商品：逻辑删除，把 status 改成 0。
        if (id == null) {
            throw new RuntimeException("商品ID不能为空");
        }

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        product.setStatus(0);
        productMapper.updateById(product);
    }

    @Override
    public void updateCover(Long id, String cover) {
        // 单独更新商品封面，供文件上传后回填图片地址使用。
        if (id == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (!StringUtils.hasText(cover)) {
            throw new RuntimeException("商品封面URL不能为空");
        }

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        product.setCover(cover);
        productMapper.updateById(product);
    }
}
