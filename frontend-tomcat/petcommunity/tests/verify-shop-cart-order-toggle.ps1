$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$shop = [System.IO.File]::ReadAllText((Join-Path $root 'shop.html'), [System.Text.Encoding]::UTF8)
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

$viewOrdersText = -join ([char]0x67E5, [char]0x770B, [char]0x8BA2, [char]0x5355)
$cancelViewText = -join ([char]0x53D6, [char]0x6D88, [char]0x67E5, [char]0x770B)

Assert-Contains $shop 'id="orderToggleButton" data-action="toggleOrders"' 'shop page needs an order toggle button.'
Assert-Contains $shop 'id="shopListTitle"' 'shop list panel needs a switchable title.'
Assert-Contains $shop 'id="cartView"' 'shop page needs a cart view container.'
Assert-Contains $shop 'id="orderView" hidden' 'order view should be hidden by default.'
Assert-Contains $app 'toggleOrders: toggleOrders' 'order toggle action is not registered.'
Assert-Contains $app 'function toggleOrders(button)' 'order toggle behavior is missing.'
Assert-Contains $app 'cartView.hidden = showOrders;' 'order view must hide the cart.'
Assert-Contains $app 'orderView.hidden = !showOrders;' 'cart view must hide the orders.'
Assert-Contains $app ('button.textContent = showOrders ? "' + $cancelViewText + '" : "' + $viewOrdersText + '";') 'order toggle button text does not switch in both directions.'

Write-Host 'Shop cart/order toggle checks passed.'
