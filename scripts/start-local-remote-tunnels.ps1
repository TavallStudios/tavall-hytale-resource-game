param(
    [string]$PostgresTunnelScript = "C:\\Users\\TJ\\Documents\\.ssh\\postgres_novus_tunnel.ps1",
    [string]$RedisTunnelScript = "C:\\Users\\TJ\\Documents\\.ssh\\redis_novus_tunnel.ps1"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $PostgresTunnelScript)) {
    throw "Postgres tunnel script not found at $PostgresTunnelScript"
}
if (-not (Test-Path $RedisTunnelScript)) {
    throw "Redis tunnel script not found at $RedisTunnelScript"
}

Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit", "-File", $PostgresTunnelScript) | Out-Null
Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoExit", "-File", $RedisTunnelScript) | Out-Null

Write-Host "Started Postgres and Redis tunnels."