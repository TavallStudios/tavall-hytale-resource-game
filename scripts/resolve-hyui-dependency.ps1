param(
    [string]$RepoRoot = "",
    [string]$OutputDirectory = "",
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent $PSScriptRoot
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $RepoRoot "target\hyui"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$mavenCommand = "mvn.cmd"
if (-not (Get-Command $mavenCommand -ErrorAction SilentlyContinue)) {
    $mavenCommand = "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"
}

$artifact = "curse.maven:hyui-1431415:7820303"
Push-Location $RepoRoot
try {
    & $mavenCommand `
        -q `
        org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy `
        "-Dartifact=$artifact" `
        "-DoutputDirectory=$OutputDirectory" `
        "-Dmdep.stripVersion=false" `
        "-DremoteRepositories=cursemaven::default::https://www.cursemaven.com"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to resolve HyUI dependency with Maven."
    }
} finally {
    Pop-Location
}

$resolvedJar = Get-ChildItem -Path $OutputDirectory -Filter "hyui-1431415-7820303*.jar" -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
if (-not $resolvedJar) {
    throw "HyUI dependency copy did not produce a jar in $OutputDirectory"
}

$stableJarPath = Join-Path $OutputDirectory "HyUI-0.9.5-all.jar"
if ($resolvedJar.FullName -ne $stableJarPath) {
    Copy-Item -Path $resolvedJar.FullName -Destination $stableJarPath -Force
}

function Read-HyUiManifest {
    param([string]$JarPath)

    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq "manifest.json" } | Select-Object -First 1
        if (-not $entry) {
            throw "HyUI jar is missing manifest.json"
        }
        $stream = $entry.Open()
        try {
            $reader = [System.IO.StreamReader]::new($stream, [System.Text.UTF8Encoding]::new($false), $true)
            try {
                return $reader.ReadToEnd() | ConvertFrom-Json
            } finally {
                $reader.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

$manifest = Read-HyUiManifest -JarPath $stableJarPath
$dependencyId = "{0}:{1}" -f $manifest.Group, $manifest.Name
if ($dependencyId -ne "Ellie:HyUI") {
    throw "Unexpected HyUI manifest identity: $dependencyId"
}
if ($manifest.ServerVersion -ne "2026.03.26-89796e57b") {
    throw "Unexpected HyUI server version: $($manifest.ServerVersion)"
}

if ($Quiet) {
    Write-Output $stableJarPath
    return
}

[ordered]@{
    artifact = $artifact
    jarPath = $stableJarPath
    dependencyId = $dependencyId
    version = $manifest.Version
    serverVersion = $manifest.ServerVersion
} | ConvertTo-Json -Depth 4
