$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$node = 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
$test = Join-Path $root 'tests\store-map-card-focus.test.js'
$styles = [IO.File]::ReadAllText((Join-Path $root 'assets\styles.css'))

& $node $test
if ($LASTEXITCODE -ne 0) {
    throw 'store map card focus behavior failed'
}
if (-not $styles.Contains('.store-card-highlight')) {
    throw 'missing store card highlight style'
}

Write-Host 'Store map card focus checks passed.'
