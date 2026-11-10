$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$services = [IO.File]::ReadAllText((Join-Path $root 'services.html'))
$profile = [IO.File]::ReadAllText((Join-Path $root 'profile.html'))
$app = [IO.File]::ReadAllText((Join-Path $root 'assets\app.js'))
$storePath = Join-Path $root 'store-register.html'
$q = [char]34
function Has($text, $marker) { if (-not $text.Contains($marker)) { throw ('missing: ' + $marker) } }
if (-not (Test-Path $storePath)) { throw 'missing store-register.html' }
$store = [IO.File]::ReadAllText($storePath)
Has $services 'api.map.baidu.com/api?v=4.0'
Has $services 'rqg5KGWLvulQC2KW0fxqSWwYT3E0In0D'
Has $services ('id=' + $q + 'nearbyStoreMap' + $q)
Has $services ('id=' + $q + 'longitude' + $q + ' type=' + $q + 'hidden' + $q)
Has $services ('id=' + $q + 'latitude' + $q + ' type=' + $q + 'hidden' + $q)
if ($services.Contains('storeForm')) { throw 'store form must leave services page' }
Has $profile 'storeRegistrationLink'
Has $profile 'store-register.html'
Has $profile ('id=' + $q + 'storeRegistrationLink' + $q + ' class=' + $q + 'button-link' + $q + ' href=' + $q + 'store-register.html' + $q + ' hidden')
Has $store 'store-register'
Has $store 'storeLocationMap'
Has $store ('name=' + $q + 'longitude' + $q + ' type=' + $q + 'hidden' + $q)
Has $store ('name=' + $q + 'latitude' + $q + ' type=' + $q + 'hidden' + $q)
foreach ($marker in @('requireMerchantStoreRegistration()', 'initStoreRegisterPage()', 'initNearbyStoreMap()', 'initStoreLocationMap()', 'new BMapGL.Geolocation()', 'addEventListener(')) { Has $app $marker }
Has $app ('page === ' + $q + 'store-register' + $q + ' && !requireMerchantStoreRegistration()')
Has $app 'link.hidden = !ownProfile || !isApprovedMerchant();'
Write-Host 'Baidu map and store registration checks passed.'
