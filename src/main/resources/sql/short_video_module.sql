USE pet_community;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'post'
      AND column_name = 'content_type'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE post ADD COLUMN content_type VARCHAR(20) NOT NULL DEFAULT ''POST'' COMMENT ''POST普通帖子 VIDEO短视频'' AFTER topic',
    'SELECT ''post.content_type 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'post'
      AND column_name = 'video_url'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE post ADD COLUMN video_url VARCHAR(500) DEFAULT NULL COMMENT ''短视频MP4地址'' AFTER content_type',
    'SELECT ''post.video_url 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'post'
      AND index_name = 'idx_post_content_status_time'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE post ADD INDEX idx_post_content_status_time (content_type, status, create_time)',
    'SELECT ''idx_post_content_status_time 已存在'''
);

PREPARE add_index_stmt FROM @add_index_sql;
EXECUTE add_index_stmt;
DEALLOCATE PREPARE add_index_stmt;
