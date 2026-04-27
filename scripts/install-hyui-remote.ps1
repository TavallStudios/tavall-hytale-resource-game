param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteModsDir = "/srv/hytale-startup-patch-test/Server/mods",
    [string]$RepoRoot = "",
    [string]$HyUiJarPath = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent $PSScriptRoot
}
if ([string]::IsNullOrWhiteSpace($HyUiJarPath)) {
    $HyUiJarPath = powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "resolve-hyui-dependency.ps1") -RepoRoot $RepoRoot -Quiet
}
if (-not (Test-Path $HyUiJarPath)) {
    throw "HyUI jar not found at $HyUiJarPath"
}

$remoteJarPath = "$RemoteModsDir/HyUI-0.9.5-all.jar"
& ssh.exe -F "C:\Users\TJ\.ssh\config" $SshAlias "mkdir -p '$RemoteModsDir'"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create remote HyUI mods directory: $RemoteModsDir"
}

& scp.exe -F "C:\Users\TJ\.ssh\config" $HyUiJarPath ("{0}:{1}" -f $SshAlias, $remoteJarPath)
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy HyUI jar to $remoteJarPath"
}

Write-Host "Copied HyUI jar to ${SshAlias}:$remoteJarPath"
