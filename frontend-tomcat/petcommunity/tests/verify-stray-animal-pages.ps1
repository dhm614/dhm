$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")

function Read-Utf8([string]$path) {
    return Get-Content -Raw -Encoding UTF8 -LiteralPath $path
}

function Assert-Contains([string]$content, [string]$marker, [string]$message) {
    if (-not $content.Contains($marker)) {
        throw $message
    }
}

$animalsPath = Join-Path $root "stray-animals.html"
$reportPath = Join-Path $root "stray-report.html"
if (-not (Test-Path -LiteralPath $animalsPath)) { throw "Missing stray-animals.html" }
if (-not (Test-Path -LiteralPath $reportPath)) { throw "Missing stray-report.html" }

$animals = Read-Utf8 $animalsPath
$report = Read-Utf8 $reportPath
$app = Read-Utf8 (Join-Path $root "assets\app.js")

foreach ($item in @(
    @($animals, 'data-page="stray-animals"'),
    @($animals, 'id="strayAnimalMap"'),
    @($animals, 'id="strayAnimalList"'),
    @($animals, 'id="strayAnimalDetail"'),
    @($animals, 'id="strayAdoptionForm"'),
    @($animals, 'id="myStrayReportList"'),
    @($animals, 'id="myAdoptionApplicationList"'),
    @($report, 'data-page="stray-report"'),
    @($report, 'id="strayReportForm"'),
    @($report, 'id="strayReportMap"'),
    @($report, 'id="strayAnimalPhotoFile"'),
    @($report, 'https://api.map.baidu.com/api?v=4.0&ak=rqg5KGWLvulQC2KW0fxqSWwYT3E0In0D')
)) {
    Assert-Contains $item[0] $item[1] "Missing stray page marker: $($item[1])"
}

foreach ($marker in @(
    'initStrayAnimalsPage()',
    'initStrayReportPage()',
    'locateNearbyStrayAnimals',
    'uploadStrayAnimalPhoto',
    'loadMyStrayReports',
    'loadMyAdoptionApplications',
    '/api/stray-animals/nearby',
    '/api/stray-animal-reports',
    '/api/stray-adoption-applications',
    'uploadFile("strayAnimalPhotoFile", "stray-animal")',
    '[data-stray-animal-id="'
)) {
    Assert-Contains $app $marker "Missing stray app marker: $marker"
}

$navPages = @(Get-ChildItem -LiteralPath $root -Filter '*.html' -File | Where-Object {
    (Read-Utf8 $_.FullName).Contains('<nav class="top-nav"')
})
foreach ($page in $navPages) {
    Assert-Contains (Read-Utf8 $page.FullName) 'data-nav-page="stray-animals" href="stray-animals.html"' "Missing stray navigation: $($page.Name)"
}

Write-Host "Stray animal page checks passed."
