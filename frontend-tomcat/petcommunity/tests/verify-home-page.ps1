$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$homePath = Join-Path $root 'home.html'
$scriptPath = Join-Path $root 'assets/app.js'
$stylePath = Join-Path $root 'assets/styles.css'

$homeText = [System.IO.File]::ReadAllText($homePath, [System.Text.Encoding]::UTF8)
$script = [System.IO.File]::ReadAllText($scriptPath, [System.Text.Encoding]::UTF8)
$style = [System.IO.File]::ReadAllText($stylePath, [System.Text.Encoding]::UTF8)

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

function U {
    param([int[]]$Codes)
    return -join ($Codes | ForEach-Object { [char]$_ })
}

$promo = U @(0x5BA0, 0x8DA3, 0x793E, 0x533A, 0xFF0C, 0x6700, 0x7528, 0x5FC3, 0x7684, 0x4E00, 0x7AD9, 0x5F0F, 0x670D, 0x52A1, 0x5E73, 0x53F0)
$hotPost = U @(0x793E, 0x533A, 0x70ED, 0x5E16)
$hotStores = U @(0x70ED, 0x95E8, 0x95E8, 0x5E97)
$hotProducts = U @(0x5546, 0x57CE, 0x70ED, 0x5356)

Assert-Contains $homeText $promo 'home page missing promo text'
Assert-Contains $homeText $hotPost 'home page missing hot post title'
Assert-Contains $homeText $hotStores 'home page missing hot stores title'
Assert-Contains $homeText $hotProducts 'home page missing hot products title'
Assert-Contains $homeText 'id="homeHotPost"' 'home page missing hot post container'
Assert-Contains $homeText 'id="homeHotStores"' 'home page missing hot stores container'
Assert-Contains $homeText 'id="homeHotProducts"' 'home page missing hot products container'
Assert-Contains $homeText 'href="community.html"' 'home page missing community link'
Assert-Contains $homeText 'href="services.html"' 'home page missing services link'
Assert-Contains $homeText 'href="shop.html"' 'home page missing shop link'
Assert-Contains $script 'function initHomePage()' 'script missing home init entry'
Assert-Contains $script 'function loadHomeHighlights()' 'script missing home data loader'
Assert-Contains $style '.home-hero' 'style missing home hero styles'

Write-Host 'Home page static checks passed'
