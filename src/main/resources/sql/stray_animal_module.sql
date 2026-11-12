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
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE temporary_name='豆包' AND longitude=118.904104 AND latitude=31.908685);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '奶茶','猫','公',1,'assets/images/content/pet-naicha.jpg','南京市江宁区弘景大道99号东侧',118.909200,31.910200,NOW(),'奶牛花色，右耳有小缺口','眼部轻微分泌物',1,0,'王老师','江宁暖心动物救助协作组','025-86189999','使用诱捕笼，避免追赶','先隔离观察再驱虫','恢复后接种猫三联',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE temporary_name='奶茶' AND longitude=118.909200 AND latitude=31.910200);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '旺财','狗','公',3,'assets/images/content/pet-wangcai.jpg','南京市江宁区弘景大道99号南侧',118.901000,31.902800,NOW(),'黄色中型犬，佩戴旧项圈','后腿轻微擦伤',1,0,'王老师','江宁暖心动物救助协作组','025-86189999','两人配合使用牵引绳','伤口处理后进行驱虫','观察七天后接种联苗和狂犬疫苗',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE temporary_name='旺财' AND longitude=118.901000 AND latitude=31.902800);
INSERT INTO stray_animal (temporary_name,species,gender,estimated_age,photo_url,found_address,longitude,latitude,found_time,appearance_description,health_status,danger_level,rescue_status,professional_name,professional_organization,professional_phone,capture_instruction,deworming_advice,vaccine_advice,status)
SELECT '小灰','狗','母',1,'assets/images/content/default-post.jpg','南京市江宁区弘景大道99号西侧',118.895600,31.911500,NOW(),'灰白小型犬，较为胆小','健康状况基本稳定',0,0,'王老师','江宁暖心动物救助协作组','025-86189999','保持安静并用零食接近','常规体内外驱虫','完成健康检查后安排基础疫苗',1
WHERE NOT EXISTS (SELECT 1 FROM stray_animal WHERE temporary_name='小灰' AND longitude=118.895600 AND latitude=31.911500);
