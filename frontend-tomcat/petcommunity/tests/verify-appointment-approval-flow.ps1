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

$app = Read-Utf8 (Join-Path $root 'assets\app.js')
$pendingText = '"\u5f85\u5546\u6237\u786e\u8ba4"' | ConvertFrom-Json
$approveText = '"\u901a\u8fc7\u9884\u7ea6"' | ConvertFrom-Json
$rejectText = '"\u9a73\u56de\u9884\u7ea6"' | ConvertFrom-Json
$submittedText = '"\u9884\u7ea6\u7533\u8bf7\u5df2\u63d0\u4ea4\uff0c\u8bf7\u7b49\u5f85\u5546\u6237\u786e\u8ba4\u3002"' | ConvertFrom-Json

Assert-Contains $app ('3: "' + $pendingText + '"') 'Pending merchant confirmation status is missing.'
Assert-Contains $app ('data-appointment-status="1">' + $approveText) 'Merchant approval action is missing.'
Assert-Contains $app ('data-appointment-status="0">' + $rejectText) 'Merchant rejection action is missing.'
Assert-Contains $app 'Number(item.status) === 1' 'Approved appointment status check is missing.'
Assert-Contains $app 'Number(item.status) === 3' 'Pending requests must be cancellable by the user.'
Assert-Contains $app $submittedText 'Appointment submission message is missing.'

Write-Host 'Appointment approval flow checks passed.'
