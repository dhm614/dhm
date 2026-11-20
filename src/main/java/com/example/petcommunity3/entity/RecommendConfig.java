package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐算法参数配置（键值对），支持运行期调整权重做对比实验。
 */
@Data
@TableName("recommend_config")
public class RecommendConfig {
    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;

    private String configValue;

    private String description;

    private LocalDateTime updateTime;
}
