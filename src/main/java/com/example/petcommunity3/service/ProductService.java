package com.example.petcommunity3.service;

import com.example.petcommunity3.dto.ProductCreateDTO;
import com.example.petcommunity3.dto.ProductUpdateDTO;
import com.example.petcommunity3.entity.Product;

import java.util.List;

public interface ProductService {
    // 新增商品：校验名称、分类、价格、库存后插入 product 表。
    void create(ProductCreateDTO dto);

    // 查询商品列表：category 可选，不传时查询全部上架商品。
    List<Product> list(String category);

    // 查询商品详情：根据商品 id 查询单个商品。
    Product getDetail(Long id);

    // 修改商品：更新商品基础信息。
    void update(ProductUpdateDTO dto);

    // 下架商品：把 status 改为 0，不删除历史商品数据。
    void disable(Long id);

    // 更新商品封面：保存文件上传后返回的图片 URL。
    void updateCover(Long id, String cover);
}
