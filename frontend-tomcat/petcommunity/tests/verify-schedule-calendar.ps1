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

function Assert-NotContains {
    param(
        [string]$Content,
        [string]$Needle,
        [string]$Message
    )
    if ($Content.Contains($Needle)) {
        throw $Message
    }
}

$fillAppointmentText = -join ([char]0x5E26, [char]0x5165, [char]0x9884, [char]0x7EA6)

Assert-Contains $services 'data-action="toggleScheduleCalendar"' 'schedule panel needs an expand/collapse action.'
Assert-Contains $services 'id="scheduleCalendarContent" hidden' 'schedule calendar should be collapsed by default.'
Assert-Contains $app 'toggleScheduleCalendar: toggleScheduleCalendar' 'schedule toggle action is not registered.'
Assert-Contains $app 'function scheduleStoreName(item)' 'schedule rows need store name mapping.'
Assert-Contains $app 'function scheduleServiceName(item)' 'schedule rows need service name mapping.'
Assert-Contains $app (">$fillAppointmentText</button>") 'schedule action should clearly say it fills the appointment form.'
Assert-Contains $app 'form.scrollIntoView({behavior: "smooth", block: "start"})' 'filled schedule should reveal the appointment form.'
Assert-NotContains $app 'loadSchedules();' 'schedule loading must always be limited to one store.'

Write-Host 'Schedule calendar checks passed.'
