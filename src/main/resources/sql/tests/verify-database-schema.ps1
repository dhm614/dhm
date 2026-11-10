$ErrorActionPreference = "Stop"

$sqlPath = Resolve-Path (Join-Path $PSScriptRoot "..\pet_community_schema.sql")
$sql = Get-Content -Encoding UTF8 -Raw -LiteralPath $sqlPath
$migrationPath = Resolve-Path (Join-Path $PSScriptRoot "..\admin_user_fields.sql")
$migration = Get-Content -Encoding UTF8 -Raw -LiteralPath $migrationPath
$featureMigrationPath = Resolve-Path (Join-Path $PSScriptRoot "..\feature_19_28.sql")
$featureMigration = Get-Content -Encoding UTF8 -Raw -LiteralPath $featureMigrationPath

foreach ($field in @("admin_level", "admin_scope", "last_login_time")) {
    if ($sql -notmatch $field) {
        throw "Schema missing sys_user field: $field"
    }
    if ($migration -notmatch $field) {
        throw "Migration missing sys_user field: $field"
    }
}

foreach ($username in @("zhangsan", "lisi", "admin")) {
    if ($sql -notmatch "'$username'") {
        throw "Schema missing seed user: $username"
    }
    if ($migration -notmatch "'$username'") {
        throw "Migration missing seed user: $username"
    }
}

if ($sql -notmatch "role VARCHAR\(20\)") {
    throw "Schema missing role column"
}

if ($sql -notmatch "INSERT INTO sys_user") {
    throw "Schema missing sys_user seed insert"
}

if ($featureMigration -notmatch "USE pet_community") {
    throw "Feature migration should select pet_community database"
}
if ($featureMigration -notmatch "CREATE TABLE IF NOT EXISTS service_schedule") {
    throw "Feature migration missing service_schedule table"
}
if ($featureMigration -notmatch "service_schedule_id") {
    throw "Feature migration missing appointment.service_schedule_id"
}
foreach ($index in @("ft_post_search", "ft_product_search")) {
    if ($sql -notmatch $index) {
        throw "Schema missing fulltext index: $index"
    }
    if ($featureMigration -notmatch $index) {
        throw "Feature migration missing fulltext index: $index"
    }
}

Write-Host "Database schema check passed"
