-- 短视频推荐模块建表脚本
-- 包含：行为流水、视频聚合统计、用户兴趣画像、相似视频表
-- 执行方式：mysql -uroot -p123456 --default-character-set=utf8mb4 -D pet_community < recommend_module.sql

USE pet_community;

-- 1. 用户行为流水：曝光、播放、完播、点赞、评论、收藏、分享、跳过
CREATE TABLE IF NOT EXISTS user_behavior (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行为ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type VARCHAR(10) NOT NULL DEFAULT 'VIDEO' COMMENT '目标类型：VIDEO/POST',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    action VARCHAR(20) NOT NULL COMMENT '动作：IMPRESSION/PLAY/PLAY_END/LIKE/COMMENT/COLLECT/SHARE/SKIP',
    watch_ms INT NOT NULL DEFAULT 0 COMMENT '观看时长毫秒',
    watch_ratio DECIMAL(4,3) NOT NULL DEFAULT 0.000 COMMENT '观看比例0~1',
    scene VARCHAR(20) NOT NULL DEFAULT 'FEED' COMMENT '场景：FEED/DETAIL',
    trace_id VARCHAR(40) DEFAULT NULL COMMENT '推荐请求链路ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ub_user_time (user_id, create_time),
    KEY idx_ub_target_action (target_id, action),
    KEY idx_ub_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为流水表';

-- 2. 视频聚合统计：由定时任务从行为流水聚合而来
CREATE TABLE IF NOT EXISTS video_stat (
    video_id BIGINT PRIMARY KEY COMMENT '视频ID（post.id）',
    imp_cnt INT NOT NULL DEFAULT 0 COMMENT '曝光次数',
    play_cnt INT NOT NULL DEFAULT 0 COMMENT '播放次数',
    play_end_cnt INT NOT NULL DEFAULT 0 COMMENT '完播次数',
    like_cnt INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_cnt INT NOT NULL DEFAULT 0 COMMENT '评论数',
    collect_cnt INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    share_cnt INT NOT NULL DEFAULT 0 COMMENT '分享数',
    avg_watch_ratio DECIMAL(4,3) NOT NULL DEFAULT 0.000 COMMENT '平均观看比例',
    hot_score DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频聚合统计表';

-- 3. 用户兴趣画像：按话题/物种/作者累积权重
CREATE TABLE IF NOT EXISTS user_interest_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_type VARCHAR(20) NOT NULL COMMENT '标签类型：TOPIC/SPECIES/AUTHOR',
    tag_value VARCHAR(100) NOT NULL COMMENT '标签值',
    weight DECIMAL(10,4) NOT NULL DEFAULT 1.0000 COMMENT '兴趣权重',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tag (user_id, tag_type, tag_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户兴趣标签表';

-- 4. 相似视频：ItemCF 协同过滤结果
CREATE TABLE IF NOT EXISTS video_similar (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    video_id BIGINT NOT NULL COMMENT '源视频ID',
    similar_video_id BIGINT NOT NULL COMMENT '相似视频ID',
    score DECIMAL(8,6) NOT NULL DEFAULT 0.000000 COMMENT '相似度',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sim_pair (video_id, similar_video_id),
    KEY idx_sim_score (video_id, score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='相似视频表';
