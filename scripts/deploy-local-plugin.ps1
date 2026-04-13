param(
    [string]$ServerRoot = "F:\\Games\\Hytale\\install\\release\\package\\game\\latest\\Server",
    [switch]$Build,
    [string]$JarPath = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $repoRoot "target\\tavall-hytale-resource-game.jar"
}

if ($Build -or -not (Test-Path $JarPath)) {
    Write-Host "Building plugin jar..."
    Push-Location $repoRoot
    try {
        $exitCode = (Start-Process -FilePath "mvn.cmd" -ArgumentList @("-DskipTests", "package") -Wait -NoNewWindow -PassThru).ExitCode
        if ($exitCode -ne 0) {
            throw "Maven build failed with exit code $exitCode"
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $JarPath)) {
    throw "Plugin jar not found at $JarPath"
}

$modsDir = Join-Path $ServerRoot "mods"
if (-not (Test-Path $modsDir)) {
    New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
}

$destPath = Join-Path $modsDir "tavall-hytale-resource-game.jar"
Copy-Item -Path $JarPath -Destination $destPath -Force

Write-Host "Copied plugin jar to $destPath"