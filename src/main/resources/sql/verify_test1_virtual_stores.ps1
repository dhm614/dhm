$ErrorActionPreference = 'Stop'

$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$query = @'
SELECT username,is_merchant,merchant_apply_status,status
FROM sys_user WHERE username='test1';
SELECT COUNT(*),COUNT(DISTINCT cover),COUNT(NULLIF(description,''))
FROM store
WHERE merchant_user_id=(SELECT id FROM sys_user WHERE username='test1');
SELECT COUNT(*)
FROM store
WHERE merchant_user_id=(SELECT id FROM sys_user WHERE username='test1')
  AND longitude BETWEEN 118.89 AND 118.92
  AND latitude BETWEEN 31.89 AND 31.92;
'@

$rows = & $mysql -uroot -p123456 --default-character-set=utf8mb4 -N -B -D pet_community -e $query
if ($LASTEXITCODE -ne 0) { throw 'database verification query failed' }
if ($rows.Count -ne 3) { throw 'test1 or virtual store data is missing' }
if ($rows[0] -ne "test1`t1`t2`t1") { throw 'test1 must be an active approved merchant' }
if ($rows[1] -ne "6`t6`t6") { throw 'test1 must own six stores with distinct covers and descriptions' }
if ($rows[2] -ne '6') { throw 'all virtual stores must be near Hongjing Avenue 99' }

Write-Host 'test1 virtual store data checks passed.'
