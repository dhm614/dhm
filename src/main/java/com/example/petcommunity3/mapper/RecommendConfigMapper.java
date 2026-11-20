package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.RecommendConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendConfigMapper extends BaseMapper<RecommendConfig> {

    @Insert("INSERT INTO recommend_config(config_key, config_value, description) "
            + "VALUES(#{key}, #{value}, #{description}) "
            + "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value)")
    int upsert(@Param("key") String key,
               @Param("value") String value,
               @Param("description") String description);
}
