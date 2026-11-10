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

CALL add_column_if_missing('merchant_application', 'merchant_name', 'VARCHAR(100) NULL COMMENT ''商户名称'' AFTER user_id');
CALL add_column_if_missing('merchant_application', 'contact_name', 'VARCHAR(50) NULL COMMENT ''联系人姓名'' AFTER merchant_name');
CALL add_column_if_missing('merchant_application', 'contact_phone', 'VARCHAR(30) NULL COMMENT ''联系电话'' AFTER contact_name');
CALL add_column_if_missing('merchant_application', 'business_type', 'VARCHAR(30) NULL COMMENT ''经营类型'' AFTER contact_phone');
CALL add_column_if_missing('merchant_application', 'business_address', 'VARCHAR(255) NULL COMMENT ''经营地址'' AFTER business_type');
CALL add_column_if_missing('merchant_application', 'attachment_name', 'VARCHAR(255) NULL COMMENT ''申请附件原始名称'' AFTER reason');
CALL add_column_if_missing('merchant_application', 'attachment_url', 'VARCHAR(500) NULL COMMENT ''申请附件访问地址'' AFTER attachment_name');

DROP PROCEDURE IF EXISTS add_column_if_missing;
