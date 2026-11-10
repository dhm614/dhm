$ErrorActionPreference = 'Stop'

$envFile = 'D:\minio\petcommunity.env.ps1'
$javaHome = 'C:\Users\31334\.jdks\ms-17.0.19'
$maven = 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$logPath = Join-Path $projectRoot 'target'
$pidPath = Join-Path $logPath 'petcommunity3-minio.pid'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw "MinIO environment file not found: $envFile"
}
if (-not (Test-Path -LiteralPath $maven -PathType Leaf)) {
    throw "Maven executable not found: $maven"
}

. $envFile
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
New-Item -ItemType Directory -Path $logPath -Force | Out-Null

$listener = netstat -ano | Select-String ':8082\s+.*LISTENING'
if ($listener) {
    throw 'Port 8082 is already in use. Stop the existing backend before starting the MinIO backend.'
}

$process = Start-Process -FilePath $maven `
    -ArgumentList @('-q', 'spring-boot:run') `
    -WorkingDirectory $projectRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logPath 'petcommunity3-minio.out.log') `
    -RedirectStandardError (Join-Path $logPath 'petcommunity3-minio.err.log') `
    -PassThru

[IO.File]::WriteAllText($pidPath, [string]$process.Id, [Text.Encoding]::ASCII)
Write-Host ("MinIO backend started. PID: " + $process.Id)
