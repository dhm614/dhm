package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.UserInterestTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserInterestTagMapper extends BaseMapper<UserInterestTag> {

    @Delete("DELETE FROM user_interest_tag WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM user_interest_tag")
    int deleteAll();

    @Insert("""
            <script>
            INSERT INTO user_interest_tag (user_id, tag_type, tag_value, weight, update_time)
            VALUES
            <foreach collection="list" item="it" separator=",">
                (#{it.userId}, #{it.tagType}, #{it.tagValue}, #{it.weight}, NOW())
            </foreach>
            ON DUPLICATE KEY UPDATE weight = VALUES(weight), update_time = NOW()
            </script>
            """)
    int insertBatch(@Param("list") List<UserInterestTag> list);
}
