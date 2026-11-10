package com.example.petcommunity3.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.petcommunity3.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    @Select("""
            SELECT id,user_id,title,content,cover,topic,content_type,video_url,like_count,comment_count,collect_count,
                   is_pinned,is_featured,pinned_time,status,create_time,update_time
            FROM post
            WHERE status = 1
              AND content_type = 'POST'
              AND MATCH(title, content, topic) AGAINST(#{keyword} IN BOOLEAN MODE)
            ORDER BY is_pinned DESC, pinned_time DESC,
                     MATCH(title, content, topic) AGAINST(#{keyword} IN BOOLEAN MODE) DESC, create_time DESC
            LIMIT #{limit}
            """)
    List<Post> searchApprovedByFullText(@Param("keyword") String keyword, @Param("limit") int limit);
}
