USE pet_community;

START TRANSACTION;

UPDATE store SET cover = CASE id
    WHEN 1 THEN 'assets/images/content/store-community-center.jpg'
    WHEN 2 THEN 'assets/images/content/store-vet-hospital.jpg'
    WHEN 3 THEN 'assets/images/content/store-pet-life.jpg'
    WHEN 4 THEN 'assets/images/content/store-grooming.jpg'
    WHEN 5 THEN 'assets/images/content/store-clinic.jpg'
    WHEN 6 THEN 'assets/images/content/store-playground.jpg'
    WHEN 7 THEN 'assets/images/content/store-supplies.jpg'
    WHEN 8 THEN 'assets/images/content/store-boarding.jpg'
END WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8);

UPDATE service_item SET cover = CASE id
    WHEN 1 THEN 'assets/images/content/service-bath.jpg'
    WHEN 2 THEN 'assets/images/content/service-grooming.jpg'
    WHEN 3 THEN 'assets/images/content/service-vaccine.jpg'
    WHEN 4 THEN 'assets/images/content/service-boarding.jpg'
END WHERE id IN (1, 2, 3, 4);

UPDATE product SET
    name = CASE WHEN id = 3 THEN '猫咪益智转盘' ELSE name END,
    cover = CASE id
        WHEN 1 THEN 'assets/images/content/product-cat-food.jpg'
        WHEN 2 THEN 'assets/images/content/product-dog-treats.jpg'
        WHEN 3 THEN 'assets/images/content/product-cat-turntable.jpg'
        WHEN 4 THEN 'assets/images/content/product-pet-ball.jpg'
    END
WHERE id IN (1, 2, 3, 4);

UPDATE pet SET avatar = CASE id
    WHEN 1 THEN 'assets/images/content/pet-naicha.jpg'
    WHEN 2 THEN 'assets/images/content/pet-wangcai.jpg'
    WHEN 3 THEN 'assets/images/content/pet-doubao.jpg'
END WHERE id IN (1, 2, 3);

COMMIT;
