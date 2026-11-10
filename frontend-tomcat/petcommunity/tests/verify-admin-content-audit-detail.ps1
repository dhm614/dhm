$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$utf8 = [System.Text.Encoding]::UTF8

function Read-Utf8([string]$Path) {
    [System.IO.File]::ReadAllText($Path, $utf8)
}

function Assert-Contains([string]$Content, [string]$Marker, [string]$Message) {
    if (-not $Content.Contains($Marker)) {
        throw $Message
    }
}

$detail = Read-Utf8 (Join-Path $root 'admin-audit-detail.html')
$admin = Read-Utf8 (Join-Path $root 'admin.html')
$app = Read-Utf8 (Join-Path $root 'assets\app.js')

Assert-Contains $app 'data-audit-detail=\"post\"' 'Post audit list must open the independent detail page.'
Assert-Contains $app 'data-audit-detail="video"' 'Video audit list must open the independent detail page.'
Assert-Contains $detail 'data-page="admin-audit-detail"' 'Audit detail page marker is missing.'
Assert-Contains $detail 'id="auditContentDetail"' 'Audit content detail container is missing.'
Assert-Contains $detail 'id="auditDecisionForm"' 'Audit decision form is missing.'
Assert-Contains $detail 'data-audit-status="1"' 'Approve choice is missing.'
Assert-Contains $detail 'data-audit-status="2"' 'Reject choice is missing.'
Assert-Contains $admin 'assets/app.js?v=20261110-content1' 'Admin page must load the current audit script version.'
Assert-Contains $detail 'assets/app.js?v=20261110-content1' 'Audit detail page must load the current audit script version.'
Assert-Contains $app 'initAdminAuditDetailPage' 'Audit detail initializer is missing.'
Assert-Contains $app '/api/admin/audit/posts/' 'Post audit detail API is missing.'
Assert-Contains $app '/api/admin/audit/videos/' 'Video audit detail API is missing.'
Assert-Contains $app '<video controls preload="metadata"' 'Audit detail page must play video content.'
Assert-Contains $app 'window.location.href = "admin-audit-detail.html?type="' 'Audit list must navigate to the independent page.'

Write-Host 'Admin content audit detail checks passed.'
