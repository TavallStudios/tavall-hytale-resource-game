param(
    [string]$RepoRoot = "",
    [string]$JarPath = "",
    [string]$CompareJarPath = "",
    [switch]$Build
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent $PSScriptRoot
}
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path $RepoRoot "target\tavall-hytale-resource-game.jar"
}

function Get-LatestSourceTimestamp {
    param([string]$Path)

    $directories = @(
        (Join-Path $Path "src\main\java"),
        (Join-Path $Path "src\main\resources"),
        (Join-Path $Path "pom.xml")
    )

    $items = foreach ($candidate in $directories) {
        if (Test-Path $candidate) {
            Get-ChildItem -Path $candidate -Recurse -File -ErrorAction SilentlyContinue
        }
    }

    if (-not $items) {
        return Get-Date "2000-01-01"
    }

    return ($items | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
}

function Invoke-MavenBuild {
    param([string]$Path)

    Push-Location $Path
    try {
        $mavenCommand = "mvn.cmd"
        if (-not (Get-Command $mavenCommand -ErrorAction SilentlyContinue)) {
            $mavenCommand = "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"
        }
        $stdoutPath = [System.IO.Path]::GetTempFileName()
        $stderrPath = [System.IO.Path]::GetTempFileName()
        $process = Start-Process `
            -FilePath $mavenCommand `
            -ArgumentList @("-q", "test", "package") `
            -Wait `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath
        $stdoutLines = if (Test-Path $stdoutPath) { Get-Content -Path $stdoutPath } else { @() }
        $stderrLines = if (Test-Path $stderrPath) { Get-Content -Path $stderrPath } else { @() }
        if ($process.ExitCode -ne 0) {
            $combinedLines = @($stdoutLines + $stderrLines) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
            if ($combinedLines.Count -gt 0) {
                Write-Host "Maven build failed. Recent output:"
                $combinedLines | Select-Object -Last 120 | ForEach-Object { Write-Host $_ }
            }
            throw "Maven build failed with exit code $($process.ExitCode)"
        }
        $warningCount = (@($stdoutLines + $stderrLines) | Where-Object { $_ -match '\bWARN\b|\bWARNING\b' }).Count
        $errorCount = (@($stdoutLines + $stderrLines) | Where-Object { $_ -match '\bERROR\b|\bSEVERE\b' }).Count
        Write-Host ("Maven build completed successfully. warnings={0} errors={1}" -f $warningCount, $errorCount)
    } finally {
        foreach ($capturePath in @($stdoutPath, $stderrPath)) {
            if ($capturePath -and (Test-Path $capturePath)) {
                Remove-Item -Path $capturePath -Force
            }
        }
        Pop-Location
    }
}

function Read-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Zip,
        [string]$EntryName
    )

    $entry = $Zip.Entries | Where-Object { $_.FullName -eq $EntryName } | Select-Object -First 1
    if (-not $entry) {
        throw "Required asset-pack entry is missing: $EntryName"
    }

    $stream = $entry.Open()
    try {
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.UTF8Encoding]::new($false), $true)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
}

$latestSourceTimestamp = Get-LatestSourceTimestamp -Path $RepoRoot
$shouldBuild = $Build -or -not (Test-Path $JarPath)
if (-not $shouldBuild -and (Get-Item $JarPath).LastWriteTime -lt $latestSourceTimestamp) {
    $shouldBuild = $true
}
if ($shouldBuild) {
    Invoke-MavenBuild -Path $RepoRoot
}

if (-not (Test-Path $JarPath)) {
    throw "Built plugin jar not found at $JarPath"
}

$unsafeTokens = @(
    "BackgroundColorHover",
    "BackgroundColorPressed",
    "TextColorHover"
)

$requiredPages = @(
    "Common/UI/Custom/Pages/castle-main.ui",
    "Common/UI/Custom/Pages/castle-upgrades.ui",
    "Common/UI/Custom/Pages/castle-buildings.ui",
    "Common/UI/Custom/Pages/building-detail.ui",
    "Common/UI/Custom/Pages/interior-main.ui",
    "Common/UI/Custom/Pages/resource-node-detail.ui"
)

$zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
try {
    $manifest = Read-ZipEntryText -Zip $zip -EntryName "manifest.json"
    if ($manifest -notmatch '"IncludesAssetPack"\s*:\s*true') {
        throw "manifest.json does not enable IncludesAssetPack"
    }

    $pageEntries = $zip.Entries | Where-Object { $_.FullName -like "Common/UI/Custom/Pages/*.ui" } | Sort-Object FullName
    if (-not $pageEntries) {
        throw "No CustomUI pages were packaged into the plugin jar"
    }

    foreach ($page in $requiredPages) {
        if (-not ($pageEntries.FullName -contains $page)) {
            throw "Required CustomUI page is missing from the built jar: $page"
        }
    }

    foreach ($entry in $pageEntries) {
        $content = Read-ZipEntryText -Zip $zip -EntryName $entry.FullName
        if ([string]::IsNullOrWhiteSpace($content)) {
            throw "CustomUI page is empty: $($entry.FullName)"
        }
        if ($content[0] -eq [char]0xFEFF) {
            throw "CustomUI page has UTF-8 BOM: $($entry.FullName)"
        }
        if (-not $content.TrimStart().StartsWith("Group")) {
            throw "CustomUI page does not start with a root Group: $($entry.FullName)"
        }
        if ($content.Contains("../Common.ui")) {
            throw "CustomUI page still imports Common.ui: $($entry.FullName)"
        }
        foreach ($token in $unsafeTokens) {
            if ($content.Contains($token)) {
                throw "CustomUI page uses unsafe token '$token': $($entry.FullName)"
            }
        }
    }
} finally {
    $zip.Dispose()
}

$result = [ordered]@{
    jarPath = $JarPath
    builtAt = (Get-Item $JarPath).LastWriteTime.ToString("o")
    manifestAssetPack = $true
    pageCount = $requiredPages.Count
    requiredPages = $requiredPages
    compareJarPath = $null
    compareJarMatches = $null
}

if (-not [string]::IsNullOrWhiteSpace($CompareJarPath)) {
    if (-not (Test-Path $CompareJarPath)) {
        throw "Compare jar not found at $CompareJarPath"
    }
    $builtHash = (Get-FileHash -Algorithm SHA256 -Path $JarPath).Hash
    $compareHash = (Get-FileHash -Algorithm SHA256 -Path $CompareJarPath).Hash
    $result.compareJarPath = $CompareJarPath
    $result.compareJarMatches = ($builtHash -eq $compareHash)
    $result.builtHash = $builtHash
    $result.compareHash = $compareHash
    if ($builtHash -ne $compareHash) {
        throw "Deployed plugin jar does not match the validated build jar"
    }
}

$result | ConvertTo-Json -Depth 4
