$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$services = [System.IO.File]::ReadAllText((Join-Path $root 'services.html'), [System.Text.Encoding]::UTF8)
$app = [System.IO.File]::ReadAllText((Join-Path $root 'assets\app.js'), [System.Text.Encoding]::UTF8)

function Assert-Contains {
    param(
        [string]$Content,
        [string]$Needle,
        [string]$Message
    )
    if (-not $Content.Contains($Needle)) {
        throw $Message
    }
}

Assert-Contains $services 'id="schedulePanel"' 'schedule panel needs a stable scroll target.'
Assert-Contains $services 'id="scheduleFeedback"' 'schedule panel needs visible loading feedback.'
Assert-Contains $services 'aria-live="polite"' 'schedule feedback should announce updates.'
Assert-Contains $app 'byId("scheduleFeedback")' 'schedule loading does not update its local feedback.'
Assert-Contains $app 'scheduleFeedback.textContent =' 'schedule loading does not render a result message.'
Assert-Contains $app 'state.schedules.length' 'schedule feedback should distinguish empty and non-empty results.'
Assert-Contains $app 'byId("schedulePanel")' 'schedule loading does not locate the schedule panel.'
Assert-Contains $app 'scrollIntoView({behavior: "smooth", block: "start"})' 'schedule button should reveal the schedule panel.'

Write-Host 'Schedule button feedback checks passed.'
