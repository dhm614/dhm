USE pet_community;

-- 推荐算法可调参数：权重、多样性上限、召回规模。
-- 所有参数运行期生效（服务端 60 秒缓存），改完无需重启。
CREATE TABLE IF NOT EXISTS recommend_config (
    config_key   VARCHAR(64)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description  VARCHAR(255) DEFAULT NULL,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐算法参数配置';

INSERT INTO recommend_config (config_key, config_value, description) VALUES
    ('weight.quality',          '0.18', '内容质量（Wilson 完播率下界）权重'),
    ('weight.completeness',     '0.05', '平均完播比例权重'),
    ('weight.engagement',       '0.05', '互动强度权重'),
    ('weight.freshness',        '0.07', '新鲜度权重'),
    ('weight.interest',         '0.53', '长期兴趣匹配（话题+作者）权重'),
    ('weight.similarity',       '0.12', '短期兴趣相似度（ItemCF）权重'),
    ('diversity.max_per_author','2',    '同一作者最多占几条'),
    ('diversity.max_per_topic', '3',    '同一话题最多占几条'),
    ('seen.penalty',            '0.65', '已看过内容的打分折扣'),
    ('recall.max_candidates',   '60',   '每路召回候选条数'),
    ('cache.feed_ttl_ms',       '120000', '推荐结果缓存时长（毫秒）')
ON DUPLICATE KEY UPDATE description = VALUES(description);
