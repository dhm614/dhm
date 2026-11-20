package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.UserBehavior;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 批量写入行为流水。调用方需保证 list 非空。
     */
    @Insert("""
            <script>
            INSERT INTO user_behavior
                (user_id, target_type, target_id, action, watch_ms, watch_ratio, scene, trace_id, create_time)
            VALUES
            <foreach collection="list" item="it" separator=",">
                (#{it.userId}, #{it.targetType}, #{it.targetId}, #{it.action},
                 #{it.watchMs}, #{it.watchRatio}, #{it.scene}, #{it.traceId}, #{it.createTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<UserBehavior> list);
}
