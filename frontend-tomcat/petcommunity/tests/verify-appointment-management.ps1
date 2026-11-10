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

$profile = Read-Utf8 (Join-Path $root 'profile.html')
$services = Read-Utf8 (Join-Path $root 'services.html')
$myAppointments = Read-Utf8 (Join-Path $root 'my-appointments.html')
$merchantAppointments = Read-Utf8 (Join-Path $root 'merchant-appointments.html')
$app = Read-Utf8 (Join-Path $root 'assets\app.js')

Assert-Contains $profile 'href="my-appointments.html"' 'My appointments entry is missing.'
Assert-Contains $services 'href="merchant-appointments.html"' 'Merchant appointment entry is missing.'
Assert-Contains $myAppointments 'data-page="my-appointments"' 'My appointments page marker is missing.'
Assert-Contains $myAppointments 'id="myAppointmentList"' 'My appointments list is missing.'
Assert-Contains $merchantAppointments 'data-page="merchant-appointments"' 'Merchant appointments page marker is missing.'
Assert-Contains $merchantAppointments 'id="merchantAppointmentStoreId"' 'Merchant store selector is missing.'
Assert-Contains $merchantAppointments 'id="merchantAppointmentList"' 'Merchant appointment list is missing.'
Assert-Contains $app 'function initMyAppointmentsPage()' 'My appointments initializer is missing.'
Assert-Contains $app 'function loadMyAppointments()' 'My appointments loader is missing.'
Assert-Contains $app 'function initMerchantAppointmentsPage()' 'Merchant appointments initializer is missing.'
Assert-Contains $app 'function loadMerchantAppointments()' 'Merchant appointments loader is missing.'
Assert-Contains $app 'request("/api/appointments/user?userId="' 'User appointment request is missing.'
Assert-Contains $app 'request("/api/appointments/store?storeId="' 'Store appointment request is missing.'
Assert-Contains $app 'request("/api/appointments/" + id + "/status"' 'Merchant appointment status update is missing.'

Write-Host 'Appointment management checks passed.'
