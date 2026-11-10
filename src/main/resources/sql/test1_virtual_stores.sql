USE pet_community;

-- 密码为 123456。test1 作为已审核通过的商户账号使用。
INSERT INTO sys_user
    (username, password, nickname, avatar, role, admin_level, admin_scope,
     is_merchant, merchant_apply_status, status)
VALUES
    ('test1', '$2a$10$CRXlYfttivTnF5748zDhA.6D9V9x7Ahu0CyTmsijyUN.aHpKHd0yS',
     '弘景测试商户', NULL, 'USER', 0, NULL, 1, 2, 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    role = 'USER',
    is_merchant = 1,
    merchant_apply_status = 2,
    status = 1;

SET @test1_user_id = (SELECT id FROM sys_user WHERE username = 'test1' LIMIT 1);

INSERT INTO store
    (merchant_user_id, name, address, phone, cover, longitude, latitude,
     business_hours, description, status)
SELECT @test1_user_id, seed.name, seed.address, seed.phone, seed.cover,
       seed.longitude, seed.latitude, seed.business_hours, seed.description, 1
FROM (
    SELECT '弘景宠爱生活馆' AS name,
           '南京市江宁区弘景大道99号附近A区（模拟地址）' AS address,
           '025-86180001' AS phone,
           'assets/images/content/store-pet-life.jpg' AS cover,
           118.904104 AS longitude, 31.908685 AS latitude,
           '09:00-21:00' AS business_hours,
           '面向大学城养宠家庭提供日常洗护、基础护理和宠物用品咨询，采用预约制分时接待。' AS description
    UNION ALL
    SELECT '江宁萌宠洗护中心',
           '南京市江宁区弘景大道99号附近东侧商业街12号（模拟地址）',
           '025-86180002', 'assets/images/content/store-grooming.jpg',
           118.906200, 31.909400, '08:30-20:30',
           '提供猫犬洗澡、吹护、修毛和基础清洁服务，店内设置独立猫咪等候区域。'
    UNION ALL
    SELECT '景园宠物诊所',
           '南京市江宁区弘景大道与芝兰路交叉口西侧18号（模拟地址）',
           '025-86180003', 'assets/images/content/store-clinic.jpg',
           118.901900, 31.907800, '08:00-22:00',
           '提供宠物健康咨询、常规检查、疫苗记录管理和术后护理指导，不提供线上真实支付。'
    UNION ALL
    SELECT '小爪印宠物乐园',
           '南京市江宁区弘景大道99号附近文心街区6号（模拟地址）',
           '025-86180004', 'assets/images/content/store-playground.jpg',
           118.904800, 31.911200, '10:00-22:00',
           '设置宠物活动、互动训练和短时托管区域，适合中小型犬进行日常社交与基础训练。'
    UNION ALL
    SELECT '喵汪补给站',
           '南京市江宁区弘景大道99号附近大学城社区21号（模拟地址）',
           '025-86180005', 'assets/images/content/store-supplies.jpg',
           118.902500, 31.905900, '09:30-21:30',
           '主营主粮、零食、玩具和日常用品，同时提供科学喂养与用品选购建议。'
    UNION ALL
    SELECT '暖心宠物寄养馆',
           '南京市江宁区弘景大道南侧科创街8号（模拟地址）',
           '025-86180006', 'assets/images/content/store-boarding.jpg',
           118.908000, 31.906900, '08:00-21:00',
           '提供猫犬分区寄养、每日活动记录和基础状态反馈，入住前需确认疫苗及健康情况。'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM store existing
    WHERE existing.merchant_user_id = @test1_user_id
      AND existing.name = seed.name
);
