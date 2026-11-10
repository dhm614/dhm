$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$community = Get-Content -Raw -Encoding UTF8 (Join-Path $root "community.html")
$admin = Get-Content -Raw -Encoding UTF8 (Join-Path $root "admin.html")
$app = Get-Content -Raw -Encoding UTF8 (Join-Path $root "assets\app.js")

function Assert-Contains($content, $needle, $message) {
    if (-not $content.Contains($needle)) {
        throw $message
    }
}

function Assert-NotContains($content, $needle, $message) {
    if ($content.Contains($needle)) {
        throw $message
    }
}

function TextFromCodes([int[]]$codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

$submittedForAudit = TextFromCodes @(35805,39064,24050,25552,20132,23457,26680)
$adminTopicTitle = TextFromCodes @(35805,39064,23457,26680,19982,31649,29702)

Assert-NotContains $community 'name="sort"' "community topic form must not expose sort input."
Assert-Contains $admin 'id="adminTopicForm"' "admin page is missing topic create form."
Assert-Contains $admin 'id="auditTopicRows"' "admin page is missing topic audit rows."
Assert-Contains $admin 'id="adminTopicList"' "admin page is missing approved topic list."
Assert-Contains $admin $adminTopicTitle "admin page is missing topic audit management title."

Assert-Contains $app "/api/admin/audit/topics?status=0" "app script does not load pending topics."
Assert-Contains $app "/api/admin/topics" "app script does not call admin topic API."
Assert-Contains $app 'headers["X-User-Id"]' "app script does not send current user id header."
Assert-Contains $app "data-review-topic" "app script is missing topic review binding."
Assert-Contains $app "data-delete-topic" "app script is missing topic delete binding."
Assert-Contains $app "function topicAuditStatus" "app script is missing topic audit status renderer."
Assert-Contains $app $submittedForAudit "user topic creation is missing audit-submitted notice."

Write-Host "Topic audit frontend checks passed."
