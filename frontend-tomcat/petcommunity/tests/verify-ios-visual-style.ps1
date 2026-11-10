$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$stylePath = Join-Path $root 'assets/styles.css'
$scriptPath = Join-Path $root 'assets/app.js'
$indexPath = Join-Path $root 'index.html'
$homePath = Join-Path $root 'home.html'
$imagesPath = Join-Path $root 'assets/images'

$style = [System.IO.File]::ReadAllText($stylePath, [System.Text.Encoding]::UTF8)
$script = [System.IO.File]::ReadAllText($scriptPath, [System.Text.Encoding]::UTF8)
$index = [System.IO.File]::ReadAllText($indexPath, [System.Text.Encoding]::UTF8)
$homeText = [System.IO.File]::ReadAllText($homePath, [System.Text.Encoding]::UTF8)

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Needle,
        [string]$Message
    )
    if (-not $Text.Contains($Needle)) {
        throw $Message
    }
}

function Assert-FileExists {
    param(
        [string]$Path,
        [string]$Message
    )
    if (-not (Test-Path -LiteralPath $Path)) {
        throw $Message
    }
}

Assert-Contains $style '-apple-system' 'stylesheet missing iOS font stack'
Assert-Contains $style 'backdrop-filter' 'stylesheet missing iOS glass header'
Assert-Contains $style '.auth-visual' 'stylesheet missing auth visual area'
Assert-Contains $style 'pet-hero.jpg' 'stylesheet missing hero image'
Assert-Contains $script 'assets/images/pet-card.jpg' 'script missing local default card image'
Assert-Contains $script 'url.indexOf("assets/") === 0' 'script missing local asset image handling'
Assert-Contains $index 'auth-visual' 'login page missing visual image panel'
Assert-Contains $homeText 'home-hero' 'home page missing hero section'

Assert-FileExists (Join-Path $imagesPath 'pet-hero.jpg') 'missing hero image asset'
Assert-FileExists (Join-Path $imagesPath 'pet-card.jpg') 'missing default card image asset'
Assert-FileExists (Join-Path $imagesPath 'pet-store.jpg') 'missing store image asset'
Assert-FileExists (Join-Path $imagesPath 'pet-food.jpg') 'missing food image asset'

Write-Host 'iOS visual style checks passed'
