param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
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

$modsDir = Join-Path $ServerRoot "mods"
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

$destinationPath = Join-Path $modsDir "HyUI-0.9.5-all.jar"
Copy-Item -Path $HyUiJarPath -Destination $destinationPath -Force
Write-Host "Copied HyUI jar to $destinationPath"
