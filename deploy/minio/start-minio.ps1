$ErrorActionPreference = 'Stop'

$envFile = 'D:\minio\petcommunity.env.ps1'
$minioExe = 'D:\minio\bin\minio.exe'
$dataPath = 'D:\minio\data'
$logPath = 'D:\minio\logs'
$pidPath = 'D:\minio\minio.pid'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw "MinIO environment file not found: $envFile"
}
if (-not (Test-Path -LiteralPath $minioExe -PathType Leaf)) {
    throw "MinIO executable not found: $minioExe"
}

. $envFile
New-Item -ItemType Directory -Path $dataPath, $logPath -Force | Out-Null

$listener = netstat -ano | Select-String ':9000\s+.*LISTENING'
if ($listener) {
    Write-Host 'MinIO API is already listening on port 9000.'
    exit 0
}

$process = Start-Process -FilePath $minioExe `
    -ArgumentList @('server', $dataPath, '--console-address', ':9001') `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logPath 'minio.out.log') `
    -RedirectStandardError (Join-Path $logPath 'minio.err.log') `
    -PassThru

[IO.File]::WriteAllText($pidPath, [string]$process.Id, [Text.Encoding]::ASCII)
Write-Host ("MinIO started. PID: " + $process.Id)
