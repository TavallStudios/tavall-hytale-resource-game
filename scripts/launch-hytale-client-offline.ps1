param(
    [string]$ClientPath = "F:\\Games\\Hytale\\install\\release\\package\\game\\latest\\Client\\HytaleClient.exe",
    [switch]$SkipEnv
)

$ErrorActionPreference = "Stop"

if (-not $SkipEnv) {
    . (Join-Path $PSScriptRoot "set-local-remote-env.ps1")
}

if (-not (Test-Path $ClientPath)) {
    throw "Hytale client not found at $ClientPath"
}

Start-Process -FilePath $ClientPath | Out-Null
Write-Host "Launched Hytale client. Start a singleplayer world and open it to LAN."