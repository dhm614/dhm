$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$checkoutPath = Join-Path $root 'checkout.html'
if (-not (Test-Path -LiteralPath $checkoutPath)) {
    throw 'Missing checkout.html.'
}

$shop = [System.IO.File]::ReadAllText((Join-Path $root 'shop.html'), [System.Text.Encoding]::UTF8)
$checkout = [System.IO.File]::ReadAllText($checkoutPath, [System.Text.Encoding]::UTF8)
$app = [System.IO.File]::ReadAllText((Join-Path $root 'assets\app.js'), [System.Text.Encoding]::UTF8)
$styles = [System.IO.File]::ReadAllText((Join-Path $root 'assets\styles.css'), [System.Text.Encoding]::UTF8)

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

Assert-Contains $shop 'data-action="goCheckout"' 'Shop page is missing the checkout button.'
Assert-Contains $checkout 'data-page="checkout"' 'Checkout page has an invalid page marker.'
Assert-Contains $checkout 'id="checkoutItems"' 'Checkout page is missing cart item details.'
Assert-Contains $checkout 'id="checkoutTotal"' 'Checkout page is missing the order total.'
Assert-Contains $checkout 'id="checkoutForm"' 'Checkout page is missing the confirmation form.'
Assert-Contains $checkout 'id="checkoutSubmit"' 'Checkout page is missing the confirmation button.'
Assert-Contains $checkout 'id="checkoutResult"' 'Checkout page is missing the order result area.'

Assert-Contains $app 'goCheckout: goCheckout' 'Checkout navigation action is not registered.'
Assert-Contains $app 'function initCheckoutPage()' 'Checkout page initializer is missing.'
Assert-Contains $app 'function loadCheckout()' 'Checkout data loading is missing.'
Assert-Contains $app 'Promise.all([' 'Checkout should load cart and product data together.'
Assert-Contains $app 'function submitCheckout(form)' 'Checkout submission is missing.'
Assert-Contains $app 'request("/api/product-orders"' 'Checkout does not call the order API.'
Assert-Contains $app 'checkoutSubmit.disabled' 'Empty carts should disable checkout.'
Assert-Contains $styles 'grid-template-columns: 72px minmax(0, 1fr);' 'Checkout items need a narrow-screen layout.'
Assert-Contains $styles 'grid-column: 2;' 'Checkout subtotal should wrap below product details on narrow screens.'

Write-Host 'Checkout flow checks passed.'
