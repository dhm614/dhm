$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$app = [IO.File]::ReadAllText((Join-Path $root 'assets\app.js'))

function Has($text, $marker) {
    if (-not $text.Contains($marker)) {
        throw ('missing: ' + $marker)
    }
}

Has $app 'nearbyStorePoint: null'
Has $app 'renderNearbyStoreMarkers(list || []);'
Has $app 'function renderNearbyStoreMarkers(list)'
Has $app 'function addNearbyCurrentLocationMarker(map, point)'
Has $app 'new BMapGL.Label('
Has $app 'new BMapGL.InfoWindow('
Has $app 'map.openInfoWindow(infoWindow, point);'

Write-Host 'Store map marker checks passed.'
