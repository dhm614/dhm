$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$profile = Get-Content -LiteralPath (Join-Path $root 'profile.html') -Raw -Encoding UTF8
$merchantApplyPath = Join-Path $root 'merchant-apply.html'
if (-not (Test-Path -LiteralPath $merchantApplyPath)) {
    throw 'Missing merchant-apply.html.'
}
$merchantApply = Get-Content -LiteralPath $merchantApplyPath -Raw -Encoding UTF8
$services = Get-Content -LiteralPath (Join-Path $root 'services.html') -Raw -Encoding UTF8
$shop = Get-Content -LiteralPath (Join-Path $root 'shop.html') -Raw -Encoding UTF8
$admin = Get-Content -LiteralPath (Join-Path $root 'admin.html') -Raw -Encoding UTF8
$app = Get-Content -LiteralPath (Join-Path $root 'assets\app.js') -Raw -Encoding UTF8

$checks = @(
    @($profile, 'merchantApplicationPanel'),
    @($profile, 'href="merchant-apply.html"'),
    @($merchantApply, 'data-page="merchant-apply"'),
    @($merchantApply, 'id="merchantApplicationForm"'),
    @($merchantApply, 'name="merchantName"'),
    @($merchantApply, 'name="contactName"'),
    @($merchantApply, 'name="contactPhone"'),
    @($merchantApply, 'name="businessType"'),
    @($merchantApply, 'name="businessAddress"'),
    @($merchantApply, 'name="reason"'),
    @($merchantApply, 'name="attachmentName"'),
    @($merchantApply, 'name="attachmentUrl"'),
    @($merchantApply, 'accept=".doc,.docx,.pdf'),
    @($merchantApply, 'data-action="uploadMerchantApplicationAttachment"'),
    @($services, 'merchantScheduleManagement'),
    @($services, 'merchantServiceManagement'),
    @($shop, 'merchantShopActions'),
    @($admin, 'merchantApplicationRows'),
    @($admin, 'data-column="merchant-attachment"'),
    @($admin, 'productApplicationRows'),
    @($app, '/api/merchant/applications'),
    @($app, '/api/merchant/product-applications'),
    @($app, '/api/admin/merchant-applications'),
    @($app, '/api/admin/product-applications')
)

foreach ($check in $checks) {
    if ($check[0] -notmatch [regex]::Escape($check[1])) {
        throw "Missing merchant approval marker: $($check[1])"
    }
}

if ($profile -match 'id="merchantApplicationForm"') {
    throw 'Profile page must not contain the merchant application form.'
}

foreach ($marker in @('initMerchantApplyPage', 'merchantName', 'contactName', 'contactPhone', 'businessType', 'businessAddress', 'uploadMerchantApplicationAttachment', 'attachmentUrl', 'attachmentName')) {
    if ($app -notmatch [regex]::Escape($marker)) {
        throw "Missing merchant application script marker: $marker"
    }
}

foreach ($file in @('merchant-product-create.html', 'merchant-products.html')) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $file))) {
        throw "Missing merchant page: $file"
    }
}

Write-Output 'merchant product approval frontend verification passed'
