USE pet_community;

START TRANSACTION;

UPDATE store SET cover = CASE id
    WHEN 1 THEN NULL
    WHEN 2 THEN NULL
    WHEN 3 THEN 'assets/images/pet-store.jpg'
    WHEN 4 THEN 'assets/images/pet-card.jpg'
    WHEN 5 THEN 'assets/images/pet-hero.jpg'
    WHEN 6 THEN 'assets/images/pet-food.jpg'
    WHEN 7 THEN '/uploads/pet/3eb4e1b54d67464ea6e8231a7e646c5c.png'
    WHEN 8 THEN 'assets/images/pet-store-garden.jpg'
END WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8);

UPDATE service_item SET cover = NULL WHERE id IN (1, 2, 3, 4);

UPDATE product SET
    name = CASE WHEN id = 3 THEN '猫抓板' ELSE name END,
    cover = NULL
WHERE id IN (1, 2, 3, 4);

UPDATE pet SET avatar = NULL WHERE id IN (1, 2, 3);

COMMIT;
