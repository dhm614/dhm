USE pet_community;

START TRANSACTION;

UPDATE merchant_application
SET attachment_url = '/uploads/merchant-application/0c2b9c100274408d8e728145c52112ce.doc'
WHERE id = 1 AND attachment_url = 'http://localhost:9000/pet-community/merchant-application/0c2b9c100274408d8e728145c52112ce.doc';

UPDATE post SET cover = '/uploads/post/497f66a968ce4959a36956739c13e937.jpg'
WHERE id = 3 AND cover = 'http://localhost:9000/pet-community/post/497f66a968ce4959a36956739c13e937.jpg';

UPDATE post SET cover = '/uploads/post/0b4a16475b6e4edfaa1e91e3f761e628.jpg'
WHERE id = 5 AND cover = 'http://localhost:9000/pet-community/post/0b4a16475b6e4edfaa1e91e3f761e628.jpg';

UPDATE post SET cover = '/uploads/short-video-cover/e1155505a3b6480dac31c87a67aab781.png'
WHERE id = 6 AND cover = 'http://localhost:9000/pet-community/short-video-cover/e1155505a3b6480dac31c87a67aab781.png';

UPDATE post SET cover = '/uploads/short-video-cover/24a7f158486f448585e3466b475a21d6.png'
WHERE id = 7 AND cover = 'http://localhost:9000/pet-community/short-video-cover/24a7f158486f448585e3466b475a21d6.png';

UPDATE post SET cover = '/uploads/short-video-cover/3a821ec530ee406a91b03e25ef15a882.png'
WHERE id = 8 AND cover = 'http://localhost:9000/pet-community/short-video-cover/3a821ec530ee406a91b03e25ef15a882.png';

UPDATE post SET video_url = '/uploads/short-video/e5b928f9cd0a4dfd9c6170a5bbc2db3b.mp4'
WHERE id = 6 AND video_url = 'http://localhost:9000/pet-community/short-video/e5b928f9cd0a4dfd9c6170a5bbc2db3b.mp4';

UPDATE post SET video_url = '/uploads/short-video/01b05d54f3164cc48d0c1e1020a6c380.mp4'
WHERE id = 7 AND video_url = 'http://localhost:9000/pet-community/short-video/01b05d54f3164cc48d0c1e1020a6c380.mp4';

UPDATE post SET video_url = '/uploads/short-video/4eed71e20931485f9f13234a4717a7f5.mp4'
WHERE id = 8 AND video_url = 'http://localhost:9000/pet-community/short-video/4eed71e20931485f9f13234a4717a7f5.mp4';

COMMIT;
