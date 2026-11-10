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

$dashboard = Read-Utf8 (Join-Path $root 'dashboard.html')
$app = Read-Utf8 (Join-Path $root 'assets\app.js')

foreach ($id in @('todayMetrics', 'appointmentConversion', 'productTop10Rows', 'hotPostRows', 'appointmentStatusList', 'orderStatusList')) {
    Assert-Contains $dashboard ('id="' + $id + '"') ('Dashboard container is missing: ' + $id)
}

foreach ($marker in @('todayActiveUserCount', 'todayPostCount', 'todayAppointmentCount', 'todayOrderCount', 'todayOrderAmount', 'finishRate', 'userAppointmentRate', 'productTop10', 'hotPosts', 'appointmentStatus', 'orderStatus')) {
    Assert-Contains $app $marker ('Dashboard renderer is missing: ' + $marker)
}

Write-Host 'Enhanced dashboard checks passed.'
