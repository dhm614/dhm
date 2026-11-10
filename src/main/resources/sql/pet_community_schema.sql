CREATE DATABASE IF NOT EXISTS pet_community
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE pet_community;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS product_operation_application;
DROP TABLE IF EXISTS merchant_application;
DROP TABLE IF EXISTS product_order_item;
DROP TABLE IF EXISTS product_order;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS service_schedule;
DROP TABLE IF EXISTS service_item;
DROP TABLE IF EXISTS store;
DROP TABLE IF EXISTS post_collect;
DROP TABLE IF EXISTS post_like;
DROP TABLE IF EXISTS post_comment;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS topic;
DROP TABLE IF EXISTS pet;
DROP TABLE IF EXISTS content_report;
DROP TABLE IF EXISTS sensitive_word;
DROP TABLE IF EXISTS user_follow;
DROP TABLE IF EXISTS user_notification;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER普通用户，ADMIN管理员',
    admin_level TINYINT NOT NULL DEFAULT 0 COMMENT '管理员等级：0普通用户，1管理员，2超级管理员',
    admin_scope VARCHAR(50) DEFAULT NULL COMMENT '管理员权限范围：ALL全部，CONTENT内容，BUSINESS业务，STATISTICS统计',
    is_merchant TINYINT NOT NULL DEFAULT 0 COMMENT '是否已审核商户：0否，1是',
    merchant_apply_status TINYINT NOT NULL DEFAULT 0 COMMENT '商户申请状态：0未申请，1待审核，2已通过，3已驳回',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1正常',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_role_status (role, status),
    KEY idx_merchant_status (is_merchant, merchant_apply_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE merchant_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商户资格申请ID',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    merchant_name VARCHAR(100) NOT NULL COMMENT '商户名称',
    contact_name VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(30) NOT NULL COMMENT '联系电话',
    business_type VARCHAR(30) NOT NULL COMMENT '经营类型',
    business_address VARCHAR(255) NOT NULL COMMENT '经营地址',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    attachment_name VARCHAR(255) DEFAULT NULL COMMENT '申请附件原始名称',
    attachment_url VARCHAR(500) DEFAULT NULL COMMENT '申请附件访问地址',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核，1已通过，2已驳回',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核管理员ID',
    review_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    review_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_status (user_id, status),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户资格申请表';

CREATE TABLE pet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(50) NOT NULL COMMENT '宠物名称',
    species VARCHAR(20) NOT NULL COMMENT '宠物类型：猫/狗/其他',
    breed VARCHAR(50) DEFAULT NULL COMMENT '品种',
    age INT DEFAULT NULL COMMENT '年龄',
    weight DECIMAL(5,2) DEFAULT NULL COMMENT '体重kg',
    gender VARCHAR(10) DEFAULT NULL COMMENT '性别：公/母/未知',
    vaccinated TINYINT NOT NULL DEFAULT 0 COMMENT '是否接种疫苗：0否，1是',
    neutered TINYINT NOT NULL DEFAULT 0 COMMENT '是否绝育：0否，1是',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '宠物头像',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';

CREATE TABLE topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '话题ID',
    name VARCHAR(50) NOT NULL COMMENT '话题名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '话题描述',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面',
    post_count INT NOT NULL DEFAULT 0 COMMENT '帖子数量',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核，1已通过，2驳回/删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题表';

CREATE TABLE post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '发帖用户ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    topic VARCHAR(50) DEFAULT NULL COMMENT '话题',
    content_type VARCHAR(20) NOT NULL DEFAULT 'POST' COMMENT 'POST普通帖子 VIDEO短视频',
    video_url VARCHAR(500) DEFAULT NULL COMMENT '短视频MP4地址',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
    collect_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否，1是',
    is_featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0否，1是',
    pinned_time DATETIME DEFAULT NULL COMMENT '最近置顶时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核，1审核通过，2审核未通过，3封禁/下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_topic (topic),
    KEY idx_status (status),
    KEY idx_post_display (status, is_pinned, pinned_time, create_time),
    KEY idx_post_content_status_time (content_type, status, create_time),
    FULLTEXT KEY ft_post_search (title, content, topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

CREATE TABLE post_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID，用于回复',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0删除，1正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_post_id (post_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论表';

CREATE TABLE post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

CREATE TABLE post_collect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子收藏表';

CREATE TABLE store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店ID',
    merchant_user_id BIGINT DEFAULT NULL COMMENT '所属商户用户ID，空表示平台门店',
    name VARCHAR(100) NOT NULL COMMENT '门店名称',
    address VARCHAR(255) NOT NULL COMMENT '地址',
    phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    business_hours VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1营业',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_store_merchant (merchant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店表';

CREATE TABLE service_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '服务项目ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    name VARCHAR(100) NOT NULL COMMENT '服务名称',
    category VARCHAR(30) NOT NULL COMMENT '分类：洗护/寄养/诊疗',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    duration INT DEFAULT NULL COMMENT '服务时长，分钟',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务项目表';

CREATE TABLE service_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排班ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    service_item_id BIGINT NOT NULL COMMENT '服务项目ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    capacity INT NOT NULL DEFAULT 1 COMMENT '可预约容量',
    booked_count INT NOT NULL DEFAULT 0 COMMENT '已预约数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1可用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_store_service_date (store_id, service_item_id, schedule_date),
    KEY idx_status_date (status, schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务排班表';

CREATE TABLE appointment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pet_id BIGINT NOT NULL COMMENT '宠物ID',
    store_id BIGINT NOT NULL COMMENT '门店ID',
    service_item_id BIGINT NOT NULL COMMENT '服务项目ID',
    service_schedule_id BIGINT DEFAULT NULL COMMENT '排班ID',
    appointment_time DATETIME NOT NULL COMMENT '预约时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status TINYINT NOT NULL DEFAULT 3 COMMENT '状态：0已取消/驳回，1已预约，2已完成，3待商户确认',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_store_id (store_id),
    KEY idx_schedule_id (service_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    merchant_user_id BIGINT DEFAULT NULL COMMENT '所属商户用户ID，空表示平台商品',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category VARCHAR(30) NOT NULL COMMENT '分类：主粮/零食/用品',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    cover VARCHAR(255) DEFAULT NULL COMMENT '封面',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    sales INT NOT NULL DEFAULT 0 COMMENT '销量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_merchant (merchant_user_id),
    FULLTEXT KEY ft_product_search (name, category, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE product_operation_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品操作申请ID',
    merchant_user_id BIGINT NOT NULL COMMENT '申请商户用户ID',
    product_id BIGINT DEFAULT NULL COMMENT '目标商品ID，新增时为空',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：CREATE/UPDATE/ENABLE/DISABLE',
    product_name VARCHAR(100) DEFAULT NULL COMMENT '商品名称快照',
    category VARCHAR(30) DEFAULT NULL COMMENT '商品分类快照',
    price DECIMAL(10,2) DEFAULT NULL COMMENT '商品价格快照',
    stock INT DEFAULT NULL COMMENT '商品库存快照',
    cover VARCHAR(255) DEFAULT NULL COMMENT '商品封面快照',
    description VARCHAR(500) DEFAULT NULL COMMENT '商品说明快照',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核，1已通过，2已驳回',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核管理员ID',
    review_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    review_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_application_merchant (merchant_user_id, status),
    KEY idx_product_application_target (product_id, status),
    KEY idx_product_application_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品操作申请表';

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CREATE TABLE product_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    remark VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已取消，1已下单，2已完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单表';

CREATE TABLE product_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单明细ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    product_price DECIMAL(10,2) NOT NULL COMMENT '商品价格快照',
    quantity INT NOT NULL COMMENT '购买数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品订单明细表';

CREATE TABLE content_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
    target_type VARCHAR(20) NOT NULL COMMENT '举报对象类型：POST/COMMENT',
    target_id BIGINT NOT NULL COMMENT '举报对象ID',
    user_id BIGINT NOT NULL COMMENT '举报用户ID',
    reason VARCHAR(255) NOT NULL COMMENT '举报原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待处理，1已处理，2已驳回',
    handle_result VARCHAR(255) DEFAULT NULL COMMENT '处理结果',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_target (target_type, target_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容举报表';

CREATE TABLE sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '敏感词ID',
    word VARCHAR(50) NOT NULL COMMENT '敏感词',
    level TINYINT NOT NULL DEFAULT 1 COMMENT '等级：1普通，2严重',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

CREATE TABLE user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
    follower_id BIGINT NOT NULL COMMENT '关注者用户ID',
    followed_id BIGINT NOT NULL COMMENT '被关注者用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_followed (follower_id, followed_id),
    KEY idx_followed_id (followed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

CREATE TABLE user_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收通知用户ID',
    from_user_id BIGINT DEFAULT NULL COMMENT '触发通知用户ID',
    type VARCHAR(30) NOT NULL COMMENT '通知类型',
    content VARCHAR(255) NOT NULL COMMENT '通知内容',
    target_type VARCHAR(30) DEFAULT NULL COMMENT '关联对象类型',
    target_id BIGINT DEFAULT NULL COMMENT '关联对象ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_read (user_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知表';

-- 初始账号密码均为 123456，密码使用 BCrypt 加密，适配后端 BCryptPasswordEncoder。
INSERT INTO sys_user
    (id, username, password, nickname, avatar, role, admin_level, admin_scope, is_merchant, merchant_apply_status, status)
VALUES
    (1, 'zhangsan', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS', '张三', NULL, 'USER', 0, NULL, 0, 0, 1),
    (2, 'lisi', '$2a$10$mTfFwzPJSdTuKtZWKYM5ZOmu3mriTBjHdMzCDl/GOCKDSAtjCoYs6', '李四', NULL, 'USER', 0, NULL, 1, 2, 1),
    (3, 'admin', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS', '管理员', NULL, 'ADMIN', 2, 'ALL', 0, 0, 1),
    (4, 'test1', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS', '弘景测试商户', NULL, 'USER', 0, NULL, 1, 2, 1);

-- 南京江宁区弘景大道99号附近的虚拟门店，坐标使用百度地图坐标系。
INSERT INTO store
    (merchant_user_id, name, address, phone, cover, longitude, latitude,
     business_hours, description, status)
VALUES
    (4, '弘景宠爱生活馆', '南京市江宁区弘景大道99号附近A区（模拟地址）',
     '025-86180001', 'assets/images/content/store-pet-life.jpg', 118.904104, 31.908685,
     '09:00-21:00', '面向大学城养宠家庭提供日常洗护、基础护理和宠物用品咨询，采用预约制分时接待。', 1),
    (4, '江宁萌宠洗护中心', '南京市江宁区弘景大道99号附近东侧商业街12号（模拟地址）',
     '025-86180002', 'assets/images/content/store-grooming.jpg', 118.906200, 31.909400,
     '08:30-20:30', '提供猫犬洗澡、吹护、修毛和基础清洁服务，店内设置独立猫咪等候区域。', 1),
    (4, '景园宠物诊所', '南京市江宁区弘景大道与芝兰路交叉口西侧18号（模拟地址）',
     '025-86180003', 'assets/images/content/store-clinic.jpg', 118.901900, 31.907800,
     '08:00-22:00', '提供宠物健康咨询、常规检查、疫苗记录管理和术后护理指导，不提供线上真实支付。', 1),
    (4, '小爪印宠物乐园', '南京市江宁区弘景大道99号附近文心街区6号（模拟地址）',
     '025-86180004', 'assets/images/content/store-playground.jpg', 118.904800, 31.911200,
     '10:00-22:00', '设置宠物活动、互动训练和短时托管区域，适合中小型犬进行日常社交与基础训练。', 1),
    (4, '喵汪补给站', '南京市江宁区弘景大道99号附近大学城社区21号（模拟地址）',
     '025-86180005', 'assets/images/content/store-supplies.jpg', 118.902500, 31.905900,
     '09:30-21:30', '主营主粮、零食、玩具和日常用品，同时提供科学喂养与用品选购建议。', 1),
    (4, '暖心宠物寄养馆', '南京市江宁区弘景大道南侧科创街8号（模拟地址）',
     '025-86180006', 'assets/images/content/store-boarding.jpg', 118.908000, 31.906900,
     '08:00-21:00', '提供猫犬分区寄养、每日活动记录和基础状态反馈，入住前需确认疫苗及健康情况。', 1);
