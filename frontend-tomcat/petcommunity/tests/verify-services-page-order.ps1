$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pagePath = Join-Path $root 'services.html'
$content = [System.IO.File]::ReadAllText($pagePath, [System.Text.Encoding]::UTF8)

function Index-Of {
    param(
        [string]$Needle,
        [string]$Message
    )
    $index = $content.IndexOf($Needle, [System.StringComparison]::Ordinal)
    if ($index -lt 0) {
        throw $Message
    }
    return $index
}

function Assert-Before {
    param(
        [int]$Left,
        [int]$Right,
        [string]$Message
    )
    if ($Left -ge $Right) {
        throw $Message
    }
}

$storeList = Index-Of 'id="storeList"' 'missing store list'
$appointmentForm = Index-Of 'id="appointmentForm"' 'missing appointment form'
$serviceItemList = Index-Of 'id="serviceItemList"' 'missing service item list'
$scheduleRows = Index-Of 'id="scheduleRows"' 'missing schedule table'
$serviceItemForm = Index-Of 'id="serviceItemForm"' 'missing service item form'

Assert-Before $storeList $appointmentForm 'store image list should appear before appointment form'
Assert-Before $appointmentForm $serviceItemList 'appointment form should appear before service item list'
Assert-Before $scheduleRows $serviceItemForm 'service item form should appear near the bottom'

if ($content.Contains('id="storeForm"')) {
    throw 'store onboarding should be moved to the independent page'
}

Write-Host 'Services page order checks passed'
