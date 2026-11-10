$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$pages = @(
    "index.html",
    "register.html",
    "home.html",
    "dashboard.html",
    "community.html",
    "community-publish.html",
    "community-topic-apply.html",
    "post-detail.html",
    "videos.html",
    "video-publish.html",
    "video-detail.html",
    "admin-audit-detail.html",
    "pets.html",
    "services.html",
    "shop.html",
    "checkout.html",
    "profile.html",
    "my-posts.html",
    "my-videos.html",
    "my-appointments.html",
    "merchant-appointments.html",
    "merchant-apply.html",
    "store-register.html",
    "admin.html"
)

foreach ($page in $pages) {
    $path = Join-Path $root $page
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing page file: $page"
    }
}

$login = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $root "index.html")
if ($login -notmatch 'data-login-role="USER"') {
    throw "Login page missing normal user login entry"
}
if ($login -notmatch 'data-login-role="ADMIN"') {
    throw "Login page missing admin login entry"
}
if ($login -notmatch "register\.html") {
    throw "Login page missing register link"
}
if ($login -match 'id="apiBase"') {
    throw "Login page should not expose API base input"
}
if ($login -match "backend-address-field") {
    throw "Login page should not expose backend address field"
}

$register = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $root "register.html")
if ($register -notmatch 'data-page="register"') {
    throw "Register page title is invalid"
}
if ($register -notmatch "/api/users/register") {
    throw "Register page missing register API marker"
}

$homeContent = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $root "home.html")
foreach ($target in @("dashboard.html", "community.html", "videos.html", "pets.html", "services.html", "shop.html", "profile.html", "admin.html")) {
    if ($homeContent -notmatch [regex]::Escape($target)) {
        throw "Home page missing link: $target"
    }
}
if ($homeContent -notmatch 'class="page-main home-main"') {
    throw "Home page missing highlighted content container"
}
foreach ($marker in @(
    '<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">',
    '<meta http-equiv="Pragma" content="no-cache">',
    '<meta http-equiv="Expires" content="0">'
)) {
    if (-not $homeContent.Contains($marker)) {
        throw "Home page missing no-cache marker: $marker"
    }
}

$script = Get-Content -Encoding UTF8 -Raw -LiteralPath (Join-Path $root "assets\app.js")
foreach ($marker in @("initLoginPage", "initRegisterPage", "initDashboardPage", "initCommunityPage", "initVideosPage", "initVideoPublishPage", "initVideoDetailPage", "initAdminAuditDetailPage", "initPetsPage", "initServicesPage", "initShopPage", "initCheckoutPage", "initProfilePage", "initMyPostsPage", "initMyVideosPage", "initMyAppointmentsPage", "initMerchantAppointmentsPage", "initStoreRegisterPage", "initAdminPage")) {
    if ($script -notmatch $marker) {
        throw "App script missing initializer: $marker"
    }
}

Write-Host "Frontend page structure check passed"
