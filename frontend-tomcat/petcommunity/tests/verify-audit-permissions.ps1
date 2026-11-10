$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$scriptPath = Join-Path $root 'assets/app.js'
$script = [System.IO.File]::ReadAllText($scriptPath, [System.Text.Encoding]::UTF8)

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

$pendingText = U @(0x5F85, 0x5BA1, 0x6838)
$approvedText = U @(0x5DF2, 0x901A, 0x8FC7)
$rejectedText = U @(0x5DF2, 0x9A73, 0x56DE)
$publishPendingText = U @(0x5E16, 0x5B50, 0x5DF2, 0x63D0, 0x4EA4, 0x5BA1, 0x6838)

Assert-Contains $script 'page === "dashboard" && !requireAdmin()' 'dashboard page should require admin'
Assert-Contains $script 'linkPage === "dashboard"' 'dashboard nav should be role-gated'
Assert-Contains $script '/api/admin/audit/posts?status=0' 'admin audit should default to pending posts'
Assert-Contains $script 'function postAuditStatus' 'audit page should have post audit status labels'
Assert-Contains $script $pendingText 'audit status should include pending text'
Assert-Contains $script $approvedText 'audit status should include approved text'
Assert-Contains $script $rejectedText 'audit status should include rejected text'
Assert-Contains $script $publishPendingText 'publishing a post should explain pending review'

Write-Host 'Audit and permissions checks passed'
