USE pet_community;

CREATE TABLE IF NOT EXISTS stray_animal_report (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, reporter_user_id BIGINT NOT NULL, temporary_name VARCHAR(100), species VARCHAR(30) NOT NULL,
 gender VARCHAR(20), estimated_age INT, photo_url VARCHAR(500) NOT NULL, found_address VARCHAR(255) NOT NULL,
 longitude DECIMAL(10,6) NOT NULL, latitude DECIMAL(10,6) NOT NULL, found_time DATETIME NOT NULL,
 appearance_description VARCHAR(1000) NOT NULL, health_description VARCHAR(1000) NOT NULL, emergency_level INT,
 reporter_phone VARCHAR(30) NOT NULL, status TINYINT NOT NULL DEFAULT 0, reviewer_id BIGINT, review_remark VARCHAR(500), review_time DATETIME,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_stray_report_user (reporter_user_id, create_time), KEY idx_stray_report_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stray_animal (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, report_id BIGINT, temporary_name VARCHAR(100), species VARCHAR(30) NOT NULL,
 gender VARCHAR(20), estimated_age INT, photo_url VARCHAR(500) NOT NULL, found_address VARCHAR(255) NOT NULL,
 longitude DECIMAL(10,6) NOT NULL, latitude DECIMAL(10,6) NOT NULL, found_time DATETIME NOT NULL,
 appearance_description VARCHAR(1000) NOT NULL, health_status VARCHAR(500) NOT NULL,
 danger_level INT NOT NULL DEFAULT 0, rescue_status TINYINT NOT NULL DEFAULT 0,
 professional_name VARCHAR(100) NOT NULL, professional_organization VARCHAR(150) NOT NULL,
 professional_phone VARCHAR(30) NOT NULL, capture_instruction VARCHAR(1000) NOT NULL,
 deworming_advice VARCHAR(1000) NOT NULL, vaccine_advice VARCHAR(1000) NOT NULL,
 status TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_stray_animal_report (report_id), KEY idx_stray_animal_status_rescue (status, rescue_status),
 KEY idx_stray_animal_coordinates (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stray_adoption_application (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, animal_id BIGINT NOT NULL, applicant_user_id BIGINT NOT NULL,
 contact_phone VARCHAR(30) NOT NULL, living_condition VARCHAR(1000) NOT NULL, pet_experience VARCHAR(1000),
 application_reason VARCHAR(1000) NOT NULL, status TINYINT NOT NULL DEFAULT 0, reviewer_id BIGINT,
 review_remark VARCHAR(500), review_time DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_stray_adoption_animal_status (animal_id, status), KEY idx_stray_adoption_user_status (applicant_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '豆包','猫','母',2,'assets/images/content/pet-doubao.jpg','南京市江宁区弘景大道99号北侧',118.904104,31.908685,NOW(),'橘白短毛，性格亲人','轻微消瘦，精神正常',0,0,'王老师','江宁暖心动物救助协作组','025-86189999','使用航空箱和食物引导','到站后进行体内外驱虫','健康检查后补齐猫三联与狂犬疫苗',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE longitude=118.904104 AND latitude=31.908685);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '奶茶','猫','公',1,'assets/images/content/pet-naicha.jpg','南京市江宁区弘景大道99号东侧',118.909200,31.910200,NOW(),'奶牛花色，右耳有小缺口','眼部轻微分泌物',1,0,'王老师','江宁暖心动物救助协作组','025-86189999','使用诱捕笼，避免追赶','先隔离观察再驱虫','恢复后接种猫三联',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE longitude=118.909200 AND latitude=31.910200);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '旺财','狗','公',3,'assets/images/content/pet-wangcai.jpg','南京市江宁区弘景大道99号南侧',118.901000,31.902800,NOW(),'黄色中型犬，佩戴旧项圈','后腿轻微擦伤',1,0,'王老师','江宁暖心动物救助协作组','025-86189999','两人配合使用牵引绳','伤口处理后进行驱虫','观察七天后接种联苗和狂犬疫苗',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE longitude=118.901000 AND latitude=31.902800);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '小灰','狗','母',1,'assets/images/content/default-post.jpg','南京市江宁区弘景大道99号西侧',118.895600,31.911500,NOW(),'灰白小型犬，较为胆小','健康状况基本稳定',0,0,'王老师','江宁暖心动物救助协作组','025-86189999','保持安静并用零食接近','常规体内外驱虫','完成健康检查后安排基础疫苗',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE longitude=118.895600 AND latitude=31.911500);

DELETE duplicate_row FROM stray_animal duplicate_row
INNER JOIN stray_animal keeper_row
    ON duplicate_row.longitude=keeper_row.longitude
    AND duplicate_row.latitude=keeper_row.latitude
    AND duplicate_row.id>keeper_row.id
WHERE duplicate_row.report_id IS NULL
  AND keeper_row.report_id IS NULL
  AND (
      (duplicate_row.longitude=118.904104 AND duplicate_row.latitude=31.908685)
      OR (duplicate_row.longitude=118.909200 AND duplicate_row.latitude=31.910200)
      OR (duplicate_row.longitude=118.901000 AND duplicate_row.latitude=31.902800)
      OR (duplicate_row.longitude=118.895600 AND duplicate_row.latitude=31.911500)
  );

UPDATE stray_animal
SET temporary_name='豆包', species='猫', gender='母', estimated_age=2,
    photo_url='assets/images/content/pet-doubao.jpg', found_address='南京市江宁区弘景大道99号北侧',
    appearance_description='橘白短毛，性格亲人，喜欢靠近行人讨食', health_status='轻微消瘦，精神状态正常',
    danger_level=0, professional_name='王老师', professional_organization='江宁暖心动物救助协作组',
    professional_phone='025-86189999', capture_instruction='使用航空箱和食物缓慢引导，避免突然追赶',
    deworming_advice='到站后进行体内外驱虫并隔离观察', vaccine_advice='健康检查后补齐猫三联与狂犬疫苗'
WHERE longitude=118.904104 AND latitude=31.908685;

UPDATE stray_animal
SET temporary_name='奶茶', species='猫', gender='公', estimated_age=1,
    photo_url='assets/images/content/pet-naicha.jpg', found_address='南京市江宁区弘景大道99号东侧商业街',
    appearance_description='橘色短毛，颈部有白色毛发，性格谨慎但可接近', health_status='眼部有轻微分泌物，需要清洁和观察',
    danger_level=1, professional_name='李医生', professional_organization='江宁宠物公益医疗协作点',
    professional_phone='025-86189998', capture_instruction='使用诱捕笼和湿粮引导，不要多人围堵',
    deworming_advice='先隔离观察三天，确认状态稳定后进行驱虫', vaccine_advice='恢复健康后接种猫三联并登记免疫记录'
WHERE longitude=118.909200 AND latitude=31.910200;

UPDATE stray_animal
SET temporary_name='旺财', species='狗', gender='公', estimated_age=3,
    photo_url='assets/images/content/pet-wangcai.jpg', found_address='南京市江宁区弘景大道99号南侧绿化带',
    appearance_description='黄色中型犬，佩戴旧项圈，对熟悉口令有反应', health_status='后腿有轻微擦伤，行走和进食正常',
    danger_level=1, professional_name='周师傅', professional_organization='南京安心捕捉与救助志愿队',
    professional_phone='025-86189997', capture_instruction='由两名专业人员配合牵引绳接近，先安抚再转移',
    deworming_advice='处理伤口后进行体内外驱虫', vaccine_advice='观察七天后接种犬联苗和狂犬疫苗'
WHERE longitude=118.901000 AND latitude=31.902800;

UPDATE stray_animal
SET temporary_name='小灰', species='狗', gender='母', estimated_age=1,
    photo_url='assets/images/content/default-post.jpg', found_address='南京市江宁区弘景大道99号西侧停车场',
    appearance_description='灰白小型犬，体型偏瘦，胆小，受到惊吓时会躲避', health_status='健康状况基本稳定，需要补充营养',
    danger_level=0, professional_name='陈老师', professional_organization='江宁流浪动物临时安置联络站',
    professional_phone='025-86189996', capture_instruction='保持环境安静，使用零食和软质围栏慢慢接近',
    deworming_advice='完成基础检查后进行常规体内外驱虫', vaccine_advice='完成健康检查后安排基础联苗和狂犬疫苗'
WHERE longitude=118.895600 AND latitude=31.911500;
