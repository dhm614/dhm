$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$admin = Get-Content -LiteralPath (Join-Path $root 'admin.html') -Raw -Encoding UTF8
$app = Get-Content -LiteralPath (Join-Path $root 'assets\app.js') -Raw -Encoding UTF8

$checks = @(
    @($admin, 'id="adminUserSearchForm"'),
    @($admin, 'id="adminUserRows"'),
    @($admin, 'data-action="loadAllAdminUsers"'),
    @($app, '/api/admin/users'),
    @($app, 'function loadAdminUsers'),
    @($app, 'function searchAdminUsers'),
    @($app, 'data-admin-user-action'),
    @($app, 'set-merchant'),
    @($app, 'set-normal'),
    @($app, 'disable'),
    @($app, 'enable')
)

foreach ($check in $checks) {
    if ($check[0] -notmatch [regex]::Escape($check[1])) {
        throw "Missing admin user management marker: $($check[1])"
    }
}

Write-Output 'admin user management frontend verification passed'
