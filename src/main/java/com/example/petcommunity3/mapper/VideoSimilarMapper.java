package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.VideoSimilar;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoSimilarMapper extends BaseMapper<VideoSimilar> {

    @Delete("DELETE FROM video_similar")
    int deleteAll();

    @Insert("""
            <script>
            INSERT IGNORE INTO video_similar (video_id, similar_video_id, score, update_time)
            VALUES
            <foreach collection="list" item="it" separator=",">
                (#{it.videoId}, #{it.similarVideoId}, #{it.score}, NOW())
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<VideoSimilar> list);
}
