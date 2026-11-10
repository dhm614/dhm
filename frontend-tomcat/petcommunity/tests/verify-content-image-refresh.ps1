$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$projectRoot = Split-Path -Parent (Split-Path -Parent $root)
$imageRoot = Join-Path $root 'assets/images/content'
$stylePath = Join-Path $root 'assets/styles.css'
$scriptPath = Join-Path $root 'assets/app.js'
$sourcesPath = Join-Path $root 'assets/images/SOURCES.md'
$migrationPath = Join-Path $projectRoot 'src/main/resources/sql/content_image_refresh.sql'
$rollbackPath = Join-Path $projectRoot 'src/main/resources/sql/content_image_refresh_rollback.sql'

function Assert-Contains {
    param([string]$Text, [string]$Needle, [string]$Message)
    if (-not $Text.Contains($Needle)) {
        throw $Message
    }
}

function Assert-FileExists {
    param([string]$Path, [string]$Message)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw $Message
    }
}

function U {
    param([int[]]$Codes)
    return -join ($Codes | ForEach-Object { [char]$_ })
}

$expectedImages = @(
    'hero-community.jpg',
    'default-post.jpg',
    'store-community-center.jpg',
    'store-vet-hospital.jpg',
    'store-pet-life.jpg',
    'store-grooming.jpg',
    'store-clinic.jpg',
    'store-playground.jpg',
    'store-supplies.jpg',
    'store-boarding.jpg',
    'service-bath.jpg',
    'service-grooming.jpg',
    'service-vaccine.jpg',
    'service-boarding.jpg',
    'product-cat-food.jpg',
    'product-dog-treats.jpg',
    'product-cat-turntable.jpg',
    'product-pet-ball.jpg',
    'pet-naicha.jpg',
    'pet-wangcai.jpg',
    'pet-doubao.jpg'
)

foreach ($image in $expectedImages) {
    Assert-FileExists (Join-Path $imageRoot $image) "Missing content image: $image"
}

Assert-FileExists $sourcesPath 'Missing image source manifest.'
Assert-FileExists $migrationPath 'Missing content image migration SQL.'
Assert-FileExists $rollbackPath 'Missing content image rollback SQL.'

$style = [System.IO.File]::ReadAllText($stylePath, [System.Text.Encoding]::UTF8)
$script = [System.IO.File]::ReadAllText($scriptPath, [System.Text.Encoding]::UTF8)
$sources = [System.IO.File]::ReadAllText($sourcesPath, [System.Text.Encoding]::UTF8)
$migration = [System.IO.File]::ReadAllText($migrationPath, [System.Text.Encoding]::UTF8)
$rollback = [System.IO.File]::ReadAllText($rollbackPath, [System.Text.Encoding]::UTF8)
$turntableName = U @(0x732B, 0x54AA, 0x76CA, 0x667A, 0x8F6C, 0x76D8)
$scratchBoardName = U @(0x732B, 0x6293, 0x677F)

Assert-Contains $style 'images/content/hero-community.jpg' 'Stylesheet must use the refreshed hero image.'
Assert-Contains $style 'images/content/default-post.jpg' 'Stylesheet must use the refreshed default image.'
Assert-Contains $script 'assets/images/content/store-community-center.jpg' 'Script must use the refreshed store fallback.'
Assert-Contains $script 'assets/images/content/product-cat-food.jpg' 'Script must use the refreshed product fallback.'
Assert-Contains $script 'assets/images/content/default-post.jpg' 'Script must use the refreshed post fallback.'
Assert-Contains $sources 'DominikSubocz/Pawfectcare-Website' 'Source manifest must record image origins.'
Assert-Contains $migration 'assets/images/content/store-community-center.jpg' 'Migration must update store images.'
Assert-Contains $migration 'assets/images/content/service-vaccine.jpg' 'Migration must update service images.'
Assert-Contains $migration 'assets/images/content/product-cat-turntable.jpg' 'Migration must update product images.'
Assert-Contains $migration 'assets/images/content/pet-naicha.jpg' 'Migration must update pet avatars.'
Assert-Contains $migration ("THEN '" + $turntableName + "'") 'Migration must align product 3 text with its image.'
Assert-Contains $rollback ("THEN '" + $scratchBoardName + "'") 'Rollback must restore the original product 3 text.'

Write-Host 'Content image refresh checks passed'
