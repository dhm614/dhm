USE pet_community;

CREATE TABLE IF NOT EXISTS service_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    service_item_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    booked_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_schedule_store_item_date (store_id, service_item_id, schedule_date),
    INDEX idx_schedule_status_date (status, schedule_date)
);

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND column_name = 'service_schedule_id'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE appointment ADD COLUMN service_schedule_id BIGINT NULL AFTER service_item_id',
    'SELECT ''appointment.service_schedule_id 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'idx_appointment_schedule_id'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_appointment_schedule_id ON appointment(service_schedule_id)',
    'SELECT ''idx_appointment_schedule_id 已存在'''
);

PREPARE add_index_stmt FROM @add_index_sql;
EXECUTE add_index_stmt;
DEALLOCATE PREPARE add_index_stmt;

SET @ft_post_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'post'
      AND index_name = 'ft_post_search'
);

SET @add_ft_post_sql = IF(
    @ft_post_exists = 0,
    'ALTER TABLE post ADD FULLTEXT KEY ft_post_search (title, content, topic)',
    'SELECT ''ft_post_search 已存在'''
);

PREPARE add_ft_post_stmt FROM @add_ft_post_sql;
EXECUTE add_ft_post_stmt;
DEALLOCATE PREPARE add_ft_post_stmt;

SET @ft_product_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'product'
      AND index_name = 'ft_product_search'
);

SET @add_ft_product_sql = IF(
    @ft_product_exists = 0,
    'ALTER TABLE product ADD FULLTEXT KEY ft_product_search (name, category, description)',
    'SELECT ''ft_product_search 已存在'''
);

PREPARE add_ft_product_stmt FROM @add_ft_product_sql;
EXECUTE add_ft_product_stmt;
DEALLOCATE PREPARE add_ft_product_stmt;
