package com.example.petcommunity3.mapper;

import com.example.petcommunity3.entity.Post;
import com.example.petcommunity3.entity.VideoStat;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 推荐相关的聚合与召回 SQL。
 * 聚合口径统一在 SQL 里完成，避免把全表数据拉到内存。
 */
@Mapper
public interface RecommendMapper {

    /**
     * 从行为流水重算 video_stat（近 30 天窗口）。
     * 热度分 = 加权互动 / sqrt(曝光 + 10) × 72 小时半衰新鲜度。
     */
    @Insert("""
            INSERT INTO video_stat (video_id, imp_cnt, play_cnt, play_end_cnt, like_cnt,
                                    comment_cnt, collect_cnt, share_cnt, avg_watch_ratio, hot_score)
            SELECT b.target_id,
                   SUM(b.action = 'IMPRESSION'),
                   SUM(b.action = 'PLAY'),
                   SUM(b.action = 'PLAY_END'),
                   SUM(b.action = 'LIKE'),
                   SUM(b.action = 'COMMENT'),
                   SUM(b.action = 'COLLECT'),
                   SUM(b.action = 'SHARE'),
                   COALESCE(AVG(CASE WHEN b.action IN ('PLAY', 'PLAY_END') THEN b.watch_ratio END), 0),
                   (SUM(b.action = 'LIKE') * 3 + SUM(b.action = 'COMMENT') * 5
                    + SUM(b.action = 'COLLECT') * 4 + SUM(b.action = 'SHARE') * 6
                    + SUM(b.action = 'PLAY_END') * 2)
                   / SQRT(SUM(b.action = 'IMPRESSION') + 10)
                   * EXP(-0.693147 * TIMESTAMPDIFF(HOUR, p.create_time, NOW()) / 72)
            FROM user_behavior b
            JOIN post p ON p.id = b.target_id AND p.content_type = 'VIDEO'
            WHERE b.target_type = 'VIDEO'
              AND b.create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            GROUP BY b.target_id, p.create_time
            ON DUPLICATE KEY UPDATE
                imp_cnt = VALUES(imp_cnt),
                play_cnt = VALUES(play_cnt),
                play_end_cnt = VALUES(play_end_cnt),
                like_cnt = VALUES(like_cnt),
                comment_cnt = VALUES(comment_cnt),
                collect_cnt = VALUES(collect_cnt),
                share_cnt = VALUES(share_cnt),
                avg_watch_ratio = VALUES(avg_watch_ratio),
                hot_score = VALUES(hot_score)
            """)
    int refreshVideoStat();

    @Select("""
            SELECT p.* FROM post p
            JOIN video_stat s ON s.video_id = p.id
            WHERE p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY s.hot_score DESC, p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectHot(@Param("limit") int limit);

    @Select("""
            SELECT p.* FROM post p
            JOIN user_follow f ON f.followed_id = p.user_id
            WHERE f.follower_id = #{userId}
              AND p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectFollow(@Param("userId") long userId, @Param("limit") int limit);

    @Select("""
            SELECT p.* FROM post p
            JOIN user_interest_tag t ON t.tag_type = 'TOPIC' AND t.tag_value = p.topic
            WHERE t.user_id = #{userId}
              AND p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY t.weight DESC, p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectByTopicInterest(@Param("userId") long userId, @Param("limit") int limit);

    @Select("""
            SELECT p.* FROM post p
            JOIN user_interest_tag t ON t.tag_type = 'AUTHOR' AND t.tag_value = CAST(p.user_id AS CHAR) COLLATE utf8mb4_unicode_ci
            WHERE t.user_id = #{userId}
              AND p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY t.weight DESC, p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectByAuthorInterest(@Param("userId") long userId, @Param("limit") int limit);

    @Select("""
            <script>
            SELECT p.* FROM post p
            JOIN video_similar s ON s.similar_video_id = p.id
            WHERE s.video_id IN
            <foreach collection="videoIds" item="id" open="(" separator="," close=")">#{id}</foreach>
              AND p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY s.score DESC, p.create_time DESC
            LIMIT #{limit}
            </script>
            """)
    List<Post> selectSimilar(@Param("videoIds") List<Long> videoIds, @Param("limit") int limit);

    @Select("""
            SELECT p.* FROM post p
            WHERE p.status = 1 AND p.content_type = 'VIDEO'
              AND p.create_time >= DATE_SUB(NOW(), INTERVAL 48 HOUR)
            ORDER BY p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectFresh(@Param("limit") int limit);

    @Select("""
            SELECT p.* FROM post p
            WHERE p.status = 1 AND p.content_type = 'VIDEO'
            ORDER BY p.create_time DESC
            LIMIT #{limit}
            """)
    List<Post> selectLatest(@Param("limit") int limit);

    @Select("""
            SELECT DISTINCT target_id FROM user_behavior
            WHERE user_id = #{userId} AND target_type = 'VIDEO'
              AND action IN ('PLAY_END', 'LIKE', 'COLLECT', 'COMMENT')
              AND create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            LIMIT 500
            """)
    List<Long> selectRecentPositiveVideoIds(@Param("userId") long userId);

    @Select("""
            <script>
            SELECT * FROM video_stat WHERE video_id IN
            <foreach collection="videoIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<VideoStat> selectStatsByIds(@Param("videoIds") List<Long> videoIds);

    @Delete("DELETE FROM user_behavior WHERE create_time < DATE_SUB(NOW(), INTERVAL 90 DAY)")
    int deleteOldBehaviors();
}
