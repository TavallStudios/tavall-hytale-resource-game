param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [switch]$Build,
    [string]$JarPath = "",
    [string]$HyUiJarPath = "",
    [switch]$SkipHyUiInstall
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$validatorScript = Join-Path $PSScriptRoot "validate-custom-ui-assets.ps1"
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $repoRoot "target\tavall-hytale-resource-game.jar"
}

function Get-LatestSourceTimestamp {
    param([string]$Path)

    $targets = @(
        (Join-Path $Path "src\main\java"),
        (Join-Path $Path "src\main\resources"),
        (Join-Path $Path "pom.xml")
    )

    $items = foreach ($candidate in $targets) {
        if (Test-Path $candidate) {
            Get-ChildItem -Path $candidate -Recurse -File -ErrorAction SilentlyContinue
        }
    }

    if (-not $items) {
        return Get-Date "2000-01-01"
    }

    return ($items | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
}

$shouldBuild = $Build -or -not (Test-Path $JarPath)
if (-not $shouldBuild -and (Get-Item $JarPath).LastWriteTime -lt (Get-LatestSourceTimestamp -Path $repoRoot)) {
    $shouldBuild = $true
}

if ($shouldBuild) {
    Write-Host "Building and validating plugin jar..."
    & $validatorScript -RepoRoot $repoRoot -Build | Out-Null
} else {
    & $validatorScript -RepoRoot $repoRoot -JarPath $JarPath | Out-Null
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
& $validatorScript -RepoRoot $repoRoot -JarPath $JarPath -CompareJarPath $destPath | Out-Null

if (-not $SkipHyUiInstall) {
    $installHyUiScript = Join-Path $PSScriptRoot "install-hyui-local.ps1"
    & $installHyUiScript -ServerRoot $ServerRoot -RepoRoot $repoRoot -HyUiJarPath $HyUiJarPath | Out-Null
}

Write-Host "Copied plugin jar to $destPath"
