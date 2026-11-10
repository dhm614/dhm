$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$propertiesPath = Join-Path $projectRoot 'src/main/resources/application.properties'
$startMinioPath = Join-Path $PSScriptRoot 'start-minio.ps1'
$startBackendPath = Join-Path $PSScriptRoot 'start-backend-minio.ps1'
$migrationPath = Join-Path $projectRoot 'src/main/resources/sql/minio_existing_uploads.sql'
$rollbackPath = Join-Path $projectRoot 'src/main/resources/sql/minio_existing_uploads_rollback.sql'

function Assert-FileExists {
    param([string]$Path, [string]$Message)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw $Message
    }
}

function Assert-Contains {
    param([string]$Text, [string]$Needle, [string]$Message)
    if (-not $Text.Contains($Needle)) {
        throw $Message
    }
}

Assert-FileExists $startMinioPath 'Missing MinIO startup script.'
Assert-FileExists $startBackendPath 'Missing MinIO backend startup script.'
Assert-FileExists $migrationPath 'Missing existing upload migration SQL.'
Assert-FileExists $rollbackPath 'Missing existing upload rollback SQL.'

$properties = [IO.File]::ReadAllText($propertiesPath, [Text.Encoding]::UTF8)
$startMinio = [IO.File]::ReadAllText($startMinioPath, [Text.Encoding]::UTF8)
$startBackend = [IO.File]::ReadAllText($startBackendPath, [Text.Encoding]::UTF8)
$migration = [IO.File]::ReadAllText($migrationPath, [Text.Encoding]::UTF8)
$rollback = [IO.File]::ReadAllText($rollbackPath, [Text.Encoding]::UTF8)

Assert-Contains $properties '${PETCOMMUNITY_STORAGE_TYPE:local}' 'Storage type must be environment-driven.'
Assert-Contains $properties '${PETCOMMUNITY_MINIO_ENDPOINT:}' 'MinIO endpoint must be environment-driven.'
Assert-Contains $properties '${PETCOMMUNITY_MINIO_ACCESS_KEY:}' 'MinIO access key must be environment-driven.'
Assert-Contains $properties '${PETCOMMUNITY_MINIO_SECRET_KEY:}' 'MinIO secret key must be environment-driven.'
Assert-Contains $properties '${PETCOMMUNITY_MINIO_PUBLIC_URL:}' 'MinIO public URL must be environment-driven.'
Assert-Contains $startMinio 'D:\minio\petcommunity.env.ps1' 'MinIO startup must load the external environment file.'
Assert-Contains $startMinio '-WindowStyle Hidden' 'MinIO must start in a hidden window.'
Assert-Contains $startBackend 'D:\minio\petcommunity.env.ps1' 'Backend startup must load the external environment file.'
Assert-Contains $startBackend '-WindowStyle Hidden' 'Backend must start in a hidden window.'
Assert-Contains $migration 'merchant_application' 'Migration must update merchant attachments.'
Assert-Contains $migration 'video_url' 'Migration must update short video URLs.'
Assert-Contains $rollback '/uploads/' 'Rollback must restore local upload URLs.'

Write-Host 'MinIO deployment checks passed'
