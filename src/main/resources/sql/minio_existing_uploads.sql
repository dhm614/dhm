USE pet_community;

START TRANSACTION;

UPDATE merchant_application
SET attachment_url = REPLACE(attachment_url, '/uploads/', 'http://localhost:9000/pet-community/')
WHERE id = 1 AND attachment_url LIKE '/uploads/%';

UPDATE post
SET cover = REPLACE(cover, '/uploads/', 'http://localhost:9000/pet-community/')
WHERE id IN (3, 5, 6, 7, 8) AND cover LIKE '/uploads/%';

UPDATE post
SET video_url = REPLACE(video_url, '/uploads/', 'http://localhost:9000/pet-community/')
WHERE id IN (6, 7, 8) AND video_url LIKE '/uploads/%';

COMMIT;
