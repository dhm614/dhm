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

$admin = Read-Utf8 (Join-Path $root "admin.html")
$app = Read-Utf8 (Join-Path $root "assets\app.js")
$detailPath = Join-Path $root "admin-stray-report-detail.html"
if (-not (Test-Path -LiteralPath $detailPath)) {
    throw "Missing admin-stray-report-detail.html"
}
$detail = Read-Utf8 $detailPath

foreach ($marker in @('id="strayReportAuditSection"', 'id="strayAnimalManagementSection"', 'id="strayAdoptionAuditSection"')) {
    Assert-Contains $admin $marker "Missing admin stray section: $marker"
}
Assert-Contains $detail 'id="adminStrayReportMap"' "Missing stray report audit map."
foreach ($marker in @('data-page="admin-stray-report-detail"', 'id="strayReportDetail"', 'id="strayReportReviewForm"', 'id="adminStrayReportMap"')) {
    Assert-Contains $detail $marker "Missing stray report detail marker: $marker"
}
foreach ($marker in @(
    'initAdminStrayReportDetailPage',
    'loadAdminStrayReports',
    'loadAdminStrayAnimals',
    'loadAdminStrayApplications',
    '/api/admin/stray-animal-reports',
    '/api/admin/stray-animals',
    '/api/admin/stray-adoption-applications',
    'admin-stray-report-detail.html?id='
)) {
    Assert-Contains $app $marker "Missing admin stray script marker: $marker"
}

Assert-Contains $app 'if (payload.status !== 1 && payload.status !== 2)' "Missing empty review status validation."

Write-Host "Stray admin flow checks passed."
