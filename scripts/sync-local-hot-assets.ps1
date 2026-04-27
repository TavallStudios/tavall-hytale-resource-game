param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [string]$HotPackName = "tavall-hytale-resource-game-hot-assets",
    [switch]$GenerateAssets,
    [switch]$ProcessResources,
    [switch]$WaitForAssetMonitor,
    [int]$WaitSeconds = 10
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$resourceRoot = Join-Path $repoRoot "src\main\resources"
$sourceCommon = Join-Path $resourceRoot "Common"
$sourceServer = Join-Path $resourceRoot "Server"
$targetModsRoot = Join-Path $ServerRoot "mods"
$targetPackRoot = Join-Path $targetModsRoot $HotPackName
$targetManifest = Join-Path $targetPackRoot "manifest.json"
$serverLogPath = Join-Path $ServerRoot "codex-live-start.txt"

function Resolve-ExistingOrParentPath {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (Test-Path -LiteralPath $Path) {
        return (Resolve-Path -LiteralPath $Path).Path
    }
    $parent = Split-Path -Parent $Path
    if ([string]::IsNullOrWhiteSpace($parent) -or -not (Test-Path -LiteralPath $parent)) {
        throw "Cannot resolve path or parent: $Path"
    }
    return (Resolve-Path -LiteralPath $parent).Path
}

function Assert-UnderRoot {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Child
    )

    $resolvedRoot = [System.IO.Path]::GetFullPath((Resolve-ExistingOrParentPath -Path $Root)).TrimEnd('\') + '\'
    $resolvedChild = [System.IO.Path]::GetFullPath((Resolve-ExistingOrParentPath -Path $Child)).TrimEnd('\') + '\'
    if (-not $resolvedChild.StartsWith($resolvedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to write outside target root. root=$resolvedRoot child=$resolvedChild"
    }
}

function Invoke-MavenProcessResources {
    Push-Location $repoRoot
    try {
        $mavenCommand = "mvn.cmd"
        if (-not (Get-Command $mavenCommand -ErrorAction SilentlyContinue)) {
            $mavenCommand = "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"
        }
        & $mavenCommand -q process-resources
        if ($LASTEXITCODE -ne 0) {
            throw "Maven process-resources failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Copy-DirectoryContents {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    if (-not (Test-Path -LiteralPath $Source -PathType Container)) {
        return
    }

    Assert-UnderRoot -Root $targetPackRoot -Child $Destination
    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Get-ChildItem -LiteralPath $Source -Force | Copy-Item -Destination $Destination -Recurse -Force
}

if (-not (Test-Path -LiteralPath $ServerRoot -PathType Container)) {
    throw "Local Hytale server root not found: $ServerRoot"
}
if (-not (Test-Path -LiteralPath $sourceCommon -PathType Container)) {
    throw "Common asset source folder not found: $sourceCommon"
}

if ($GenerateAssets) {
    & python (Join-Path $PSScriptRoot "generate-resource-game-assets.py")
    if ($LASTEXITCODE -ne 0) {
        throw "Asset generator failed with exit code $LASTEXITCODE"
    }
}

if ($ProcessResources -or -not (Test-Path -LiteralPath (Join-Path $repoRoot "target\classes\manifest.json"))) {
    Invoke-MavenProcessResources
}

New-Item -ItemType Directory -Force -Path $targetPackRoot | Out-Null
Assert-UnderRoot -Root $targetModsRoot -Child $targetPackRoot
$logHighWatermark = if (Test-Path -LiteralPath $serverLogPath) {
    (Get-Item -LiteralPath $serverLogPath).LastWriteTimeUtc
} else {
    $null
}

$manifest = [ordered]@{
    Group = "com.tavall.hytale"
    Name = "Tavall Hytale Resource Game Hot Assets"
    Version = "0.1.1-SNAPSHOT"
    Description = "Development hot asset pack mirror for Tavall Hytale Resource Game UI, models, and prefab recipes."
    Authors = @(
        [ordered]@{
            Name = "Tavall"
            Email = ""
            Url = ""
        }
    )
    Website = ""
    Dependencies = [ordered]@{}
    OptionalDependencies = [ordered]@{}
    LoadBefore = [ordered]@{}
    ServerVersion = "2026.03.26-89796e57b"
    DisabledByDefault = $false
    IncludesAssetPack = $false
    SubPlugins = @()
}

Copy-DirectoryContents -Source $sourceCommon -Destination (Join-Path $targetPackRoot "Common")
Copy-DirectoryContents -Source $sourceServer -Destination (Join-Path $targetPackRoot "Server")
$manifestJson = $manifest | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText($targetManifest, $manifestJson, [System.Text.UTF8Encoding]::new($false))

$assetFileCount = @(Get-ChildItem -LiteralPath $targetPackRoot -Recurse -File | Where-Object { $_.FullName -ne $targetManifest }).Count
$result = [ordered]@{
    hotPackRoot = $targetPackRoot
    manifest = $targetManifest
    assetFileCount = $assetFileCount
    generatedAssetManifest = Test-Path -LiteralPath (Join-Path $targetPackRoot "Common\UI\Custom\Textures\ResourceGame\resource-game-ui-assets.json")
    generatedModelManifest = Test-Path -LiteralPath (Join-Path $targetPackRoot "Common\Models\ResourceGame\resource-game-model-assets.json")
    waitForAssetMonitor = $WaitForAssetMonitor.IsPresent
    observedAssetMonitor = $false
}

if ($WaitForAssetMonitor) {
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    do {
        if (Test-Path -LiteralPath $serverLogPath) {
            $logInfo = Get-Item -LiteralPath $serverLogPath
            $recentLines = Get-Content -LiteralPath $serverLogPath -Tail 120 -ErrorAction SilentlyContinue
            if (($null -eq $logHighWatermark -or $logInfo.LastWriteTimeUtc -gt $logHighWatermark) -and ($recentLines -match "Reloaded|loaded|Asset|assets")) {
                $result.observedAssetMonitor = $true
                break
            }
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
}

$result | ConvertTo-Json -Depth 5
