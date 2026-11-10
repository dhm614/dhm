$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$services = [System.IO.File]::ReadAllText((Join-Path $root 'services.html'), [System.Text.Encoding]::UTF8)
$storeRegistration = [System.IO.File]::ReadAllText((Join-Path $root 'store-register.html'), [System.Text.Encoding]::UTF8)
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

Assert-Contains $storeRegistration 'id="storeCoverFile"' 'store registration page is missing store image file input.'
Assert-Contains $storeRegistration 'data-action="uploadStoreCover"' 'store registration page is missing store image upload button.'
Assert-Contains $services 'id="serviceItemCoverFile"' 'services page is missing service item image file input.'
Assert-Contains $services 'data-action="uploadServiceItemCover"' 'services page is missing service item image upload button.'

Assert-Contains $app 'uploadStoreCover: uploadStoreCover' 'app actions are missing store cover upload.'
Assert-Contains $app 'uploadServiceItemCover: uploadServiceItemCover' 'app actions are missing service item cover upload.'
Assert-Contains $app 'function uploadStoreCover()' 'app script is missing uploadStoreCover function.'
Assert-Contains $app 'function uploadServiceItemCover()' 'app script is missing uploadServiceItemCover function.'
Assert-Contains $app 'uploadFile("storeCoverFile", "store")' 'store cover upload should use the store module.'
Assert-Contains $app 'uploadFile("serviceItemCoverFile", "service")' 'service item cover upload should use the service module.'

Write-Host 'Service image upload checks passed.'
