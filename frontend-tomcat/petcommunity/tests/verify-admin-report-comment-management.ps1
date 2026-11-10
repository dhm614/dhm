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

$admin = Read-Utf8 (Join-Path $root 'admin.html')
$app = Read-Utf8 (Join-Path $root 'assets\app.js')

Assert-Contains $admin 'id="commentManagementSection"' 'Comment management section is missing.'
Assert-Contains $admin 'id="adminCommentRows"' 'Comment management table body is missing.'
Assert-Contains $app 'function loadAdminComments()' 'Admin comment loader is missing.'
Assert-Contains $app 'scrollToAdminSection("commentManagementSection")' 'Comment management button must scroll to its section.'
Assert-Contains $app 'data-admin-comment-action' 'Comment management actions are missing.'
Assert-Contains $app 'data-report-handle' 'Report handling actions are missing.'
Assert-Contains $app '/api/admin/audit/comments' 'Comment audit API is missing.'
Assert-Contains $app '"/handle"' 'Report handle API request is missing.'

Write-Host 'Admin report and comment management checks passed.'
