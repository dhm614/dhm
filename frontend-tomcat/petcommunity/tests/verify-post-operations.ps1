$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$admin = Get-Content -LiteralPath (Join-Path $root 'admin.html') -Raw -Encoding UTF8
$app = Get-Content -LiteralPath (Join-Path $root 'assets\app.js') -Raw -Encoding UTF8

foreach ($marker in @('approvedPostRows', '/api/admin/posts/', 'data-post-operation', 'pinAction', 'featureAction')) {
    $source = if ($marker -eq 'approvedPostRows') { $admin } else { $app }
    if ($source -notmatch [regex]::Escape($marker)) {
        throw "Missing post operation marker: $marker"
    }
}

Write-Output 'post operations frontend verification passed'
