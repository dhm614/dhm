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
$styles = Read-Utf8 (Join-Path $root 'assets\styles.css')

Assert-Contains $admin 'id="topicManagementSection"' 'Topic management section anchor is missing.'
Assert-Contains $admin 'id="sensitiveWordSection"' 'Sensitive word section anchor is missing.'
Assert-Contains $admin 'id="reportSection"' 'Report section anchor is missing.'
Assert-Contains $admin '<section class="panel admin-anchor-section" id="sensitiveWordSection">' 'Sensitive words must use an independent panel.'
Assert-Contains $admin '<section class="panel admin-anchor-section" id="reportSection">' 'Reports must use an independent panel.'
Assert-Contains $app 'function scrollToAdminSection(sectionId)' 'Admin section scrolling helper is missing.'
Assert-Contains $app 'scrollToAdminSection("topicManagementSection")' 'Topic button must scroll to topic management.'
Assert-Contains $app 'scrollToAdminSection("sensitiveWordSection")' 'Sensitive word button must scroll to its section.'
Assert-Contains $app 'scrollToAdminSection("reportSection")' 'Report button must scroll to its section.'
Assert-Contains $styles '.admin-anchor-section {' 'Admin anchor section style is missing.'
Assert-Contains $styles 'scroll-margin-top: 92px;' 'Admin section headings must remain visible below the sticky header.'

Write-Host 'Admin section navigation checks passed.'
