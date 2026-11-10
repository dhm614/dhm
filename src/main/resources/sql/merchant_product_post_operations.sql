USE pet_community;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', column_definition_value);
        PREPARE statement_to_run FROM @ddl;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('sys_user', 'is_merchant', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已审核商户：0否，1是'' AFTER admin_scope');
CALL add_column_if_missing('sys_user', 'merchant_apply_status', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''商户申请状态：0未申请，1待审核，2已通过，3已驳回'' AFTER is_merchant');
CALL add_column_if_missing('store', 'merchant_user_id', 'BIGINT NULL COMMENT ''所属商户用户ID'' AFTER id');
CALL add_column_if_missing('product', 'merchant_user_id', 'BIGINT NULL COMMENT ''所属商户用户ID'' AFTER id');
CALL add_column_if_missing('post', 'is_pinned', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否置顶'' AFTER collect_count');
CALL add_column_if_missing('post', 'is_featured', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否精选'' AFTER is_pinned');
CALL add_column_if_missing('post', 'pinned_time', 'DATETIME NULL COMMENT ''最近置顶时间'' AFTER is_featured');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS merchant_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    business_type VARCHAR(30) NOT NULL,
    business_address VARCHAR(255) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    attachment_name VARCHAR(255) DEFAULT NULL,
    attachment_url VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    reviewer_id BIGINT DEFAULT NULL,
    review_remark VARCHAR(255) DEFAULT NULL,
    review_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_status (user_id, status),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_operation_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_user_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    operation_type VARCHAR(20) NOT NULL,
    product_name VARCHAR(100) DEFAULT NULL,
    category VARCHAR(30) DEFAULT NULL,
    price DECIMAL(10,2) DEFAULT NULL,
    stock INT DEFAULT NULL,
    cover VARCHAR(255) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    reviewer_id BIGINT DEFAULT NULL,
    review_remark VARCHAR(255) DEFAULT NULL,
    review_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_application_merchant (merchant_user_id, status),
    KEY idx_product_application_target (product_id, status),
    KEY idx_product_application_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE sys_user
SET is_merchant = 1, merchant_apply_status = 2
WHERE username = 'lisi';

UPDATE sys_user
SET is_merchant = 0, merchant_apply_status = 0
WHERE username IN ('zhangsan', 'admin');

SET @lisi_id = (SELECT id FROM sys_user WHERE username = 'lisi' LIMIT 1);
UPDATE store SET merchant_user_id = @lisi_id WHERE merchant_user_id IS NULL AND @lisi_id IS NOT NULL;
UPDATE product SET merchant_user_id = @lisi_id WHERE merchant_user_id IS NULL AND @lisi_id IS NOT NULL;
