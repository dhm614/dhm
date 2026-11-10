param(
    [string]$MySqlPath = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$Password = '123456'
)

$ErrorActionPreference = 'Stop'

$seedPath = Join-Path $PSScriptRoot '..\mock_service_schedules.sql'
if (-not (Test-Path -LiteralPath $seedPath)) {
    throw 'Missing mock_service_schedules.sql.'
}

$seedSql = [System.IO.File]::ReadAllText($seedPath, [System.Text.Encoding]::UTF8)
if (-not $seedSql.Contains('NOT EXISTS')) {
    throw 'Schedule seed must be idempotent.'
}

$query = @"
SELECT COUNT(*)
FROM service_schedule
WHERE schedule_date BETWEEN '2026-11-11' AND '2026-11-17'
  AND status = 1;

SELECT COUNT(*)
FROM (
    SELECT service_item.id
    FROM service_item
    LEFT JOIN service_schedule
      ON service_schedule.service_item_id = service_item.id
     AND service_schedule.schedule_date BETWEEN '2026-11-11' AND '2026-11-17'
     AND service_schedule.status = 1
    WHERE service_item.status = 1
    GROUP BY service_item.id
    HAVING COUNT(service_schedule.id) <> 21
) invalid_service_item;
"@

$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $Password
    $result = & $MySqlPath '--user=root' '--default-character-set=utf8mb4' '--database=pet_community' '--batch' '--skip-column-names' '-e' $query
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to query pet_community.'
    }
} finally {
    $env:MYSQL_PWD = $previousPassword
}

$values = @($result | ForEach-Object { [int]$_ })
if ($values.Count -ne 2) {
    throw 'Unexpected verification query result.'
}
if ($values[0] -ne 84) {
    throw "Expected 84 mock schedules, found $($values[0])."
}
if ($values[1] -ne 0) {
    throw "Expected 21 schedules for every active service item; invalid items: $($values[1])."
}

Write-Host 'Mock service schedule checks passed.'
