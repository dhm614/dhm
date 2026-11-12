$ErrorActionPreference = 'Stop'
$schema = Get-Content -Raw -Encoding UTF8 -LiteralPath 'src/main/resources/sql/pet_community_schema.sql'
$migration = Get-Content -Raw -Encoding UTF8 -LiteralPath 'src/main/resources/sql/stray_animal_module.sql'
$seedMarker = [string]([char]0x5F18) + [char]0x666F + [char]0x5927 + [char]0x9053 + '99' + [char]0x53F7
$required = @('CREATE TABLE stray_animal_report','CREATE TABLE stray_animal','CREATE TABLE stray_adoption_application','idx_stray_report_status_time','idx_stray_animal_status_rescue','idx_stray_animal_coordinates','idx_stray_adoption_animal_status','idx_stray_adoption_user_status',$seedMarker)
foreach ($marker in $required) {
    if (($schema -notmatch [regex]::Escape($marker)) -and ($migration -notmatch [regex]::Escape($marker))) {
        throw "Missing stray schema marker: $marker"
    }
}
if (($migration | Select-String -Pattern 'WHERE NOT EXISTS' -AllMatches).Matches.Count -lt 4) {
    throw 'Stray seed data must be idempotent.'
}
Write-Output 'Stray database schema check passed.'
