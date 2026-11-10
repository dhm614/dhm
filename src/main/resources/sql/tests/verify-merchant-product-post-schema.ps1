$ErrorActionPreference = 'Stop'

$schemaPath = Join-Path $PSScriptRoot '..\pet_community_schema.sql'
$schema = Get-Content -LiteralPath $schemaPath -Raw -Encoding UTF8

$requiredPatterns = @(
    'is_merchant\s+TINYINT',
    'merchant_apply_status\s+TINYINT',
    'CREATE TABLE merchant_application',
    'merchant_name\s+VARCHAR',
    'contact_name\s+VARCHAR',
    'contact_phone\s+VARCHAR',
    'business_type\s+VARCHAR',
    'business_address\s+VARCHAR',
    'attachment_name\s+VARCHAR',
    'attachment_url\s+VARCHAR',
    'CREATE TABLE product_operation_application',
    'merchant_user_id\s+BIGINT',
    'is_pinned\s+TINYINT',
    'is_featured\s+TINYINT',
    'pinned_time\s+DATETIME'
)

foreach ($pattern in $requiredPatterns) {
    if ($schema -notmatch $pattern) {
        throw "Missing schema pattern: $pattern"
    }
}

Write-Output 'merchant product post schema verification passed'
