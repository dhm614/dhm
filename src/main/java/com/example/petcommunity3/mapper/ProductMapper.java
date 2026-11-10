package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("""
            SELECT id,name,category,price,stock,cover,description,sales,status,create_time,update_time
            FROM product
            WHERE status = 1
              AND MATCH(name, category, description) AGAINST(#{keyword} IN BOOLEAN MODE)
            ORDER BY MATCH(name, category, description) AGAINST(#{keyword} IN BOOLEAN MODE) DESC, sales DESC
            LIMIT #{limit}
            """)
    List<Product> searchAvailableByFullText(@Param("keyword") String keyword, @Param("limit") int limit);
}
