USE pet_community;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'admin_level'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN admin_level TINYINT NOT NULL DEFAULT 0 COMMENT ''管理员等级：0普通用户，1管理员，2超级管理员'' AFTER role',
    'SELECT ''sys_user.admin_level 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'admin_scope'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN admin_scope VARCHAR(50) NULL COMMENT ''管理员权限范围：ALL全部，CONTENT内容，BUSINESS业务，STATISTICS统计'' AFTER admin_level',
    'SELECT ''sys_user.admin_scope 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'last_login_time'
);

SET @add_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME NULL COMMENT ''最后登录时间'' AFTER status',
    'SELECT ''sys_user.last_login_time 已存在'''
);

PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND index_name = 'idx_role_status'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'CREATE INDEX idx_role_status ON sys_user(role, status)',
    'SELECT ''idx_role_status 已存在'''
);

PREPARE add_index_stmt FROM @add_index_sql;
EXECUTE add_index_stmt;
DEALLOCATE PREPARE add_index_stmt;

-- 初始账号密码均为 123456，密码使用 BCrypt 加密，适配后端 BCryptPasswordEncoder。
INSERT INTO sys_user
    (username, password, nickname, avatar, role, admin_level, admin_scope, status)
VALUES
    ('zhangsan', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS', '张三', NULL, 'USER', 0, NULL, 1),
    ('lisi', '$2a$10$mTfFwzPJSdTuKtZWKYM5ZOmu3mriTBjHdMzCDl/GOCKDSAtjCoYs6', '李四', NULL, 'USER', 0, NULL, 1),
    ('admin', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS', '管理员', NULL, 'ADMIN', 2, 'ALL', 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    role = VALUES(role),
    admin_level = VALUES(admin_level),
    admin_scope = VALUES(admin_scope),
    status = VALUES(status);
