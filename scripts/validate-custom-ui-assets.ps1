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
    "Common/UI/Custom/Pages/castle-main.html",
    "Common/UI/Custom/Pages/castle-info.html",
    "Common/UI/Custom/Pages/castle-citizens.html",
    "Common/UI/Custom/Pages/castle-troops.html",
    "Common/UI/Custom/Pages/castle-resources.html",
    "Common/UI/Custom/Pages/castle-upgrades.html",
    "Common/UI/Custom/Pages/castle-buildings.html",
    "Common/UI/Custom/Pages/building-detail.html",
    "Common/UI/Custom/Pages/interior-main.html",
    "Common/UI/Custom/Pages/resource-node-detail.html",
    "Common/UI/Custom/Pages/debug-navigator.html"
)

$requiredGeneratedAssets = @(
    "Common/UI/Custom/Textures/ResourceGame/resource-game-ui-assets.json",
    "Common/UI/Custom/Textures/ResourceGame/panels/ui_panel_castle_ledger_base.png",
    "Common/UI/Custom/Textures/ResourceGame/panels/ui_panel_war_table_base.png",
    "Common/UI/Custom/Textures/ResourceGame/panels/ui_panel_workshop_base.png",
    "Common/UI/Custom/Textures/ResourceGame/panels/ui_panel_node_detail_base.png",
    "Common/UI/Custom/Textures/ResourceGame/panels/ui_panel_interior_base.png",
    "Common/UI/Custom/Textures/ResourceGame/buttons/ui_button_primary_normal.png",
    "Common/UI/Custom/Textures/ResourceGame/buttons/ui_button_secondary_normal.png",
    "Common/UI/Custom/Textures/ResourceGame/buttons/ui_button_confirm_normal.png",
    "Common/UI/Custom/Textures/ResourceGame/buttons/ui_button_danger_normal.png",
    "Common/UI/Custom/Textures/ResourceGame/icons/ui_icon_kingdom_castle.png",
    "Common/UI/Custom/Textures/ResourceGame/icons/ui_icon_action_upgrade.png",
    "Common/UI/Custom/Textures/ResourceGame/icons/ui_icon_node_marker.png",
    "Common/UI/Custom/Textures/ResourceGame/selectors/ui_selector_building_valid.png",
    "Common/UI/Custom/Textures/ResourceGame/selectors/ui_selector_building_blocked.png",
    "Common/UI/Custom/Textures/ResourceGame/fonts/font_template_big_alphabet.png",
    "Common/UI/Custom/Textures/ResourceGame/fonts/font_template_menu_alphabet.png",
    "Common/UI/Custom/Textures/ResourceGame/fonts/font_template_numbers_symbols.png",
    "Common/Models/ResourceGame/resource-game-model-assets.json",
    "Common/Models/ResourceGame/castle_keep.bbmodel",
    "Common/Models/ResourceGame/farmstead.bbmodel",
    "Common/Models/ResourceGame/lumber_mill.bbmodel",
    "Common/Models/ResourceGame/iron_works.bbmodel",
    "Common/Models/ResourceGame/barracks.bbmodel",
    "Common/Models/ResourceGame/workshop.bbmodel",
    "Common/Models/ResourceGame/resource_node.bbmodel",
    "Common/Prefabs/ResourceGame/castle_keep.resource-prefab.json",
    "Common/Prefabs/ResourceGame/farmstead.resource-prefab.json",
    "Common/Prefabs/ResourceGame/lumber_mill.resource-prefab.json",
    "Common/Prefabs/ResourceGame/iron_works.resource-prefab.json",
    "Common/Prefabs/ResourceGame/barracks.resource-prefab.json",
    "Common/Prefabs/ResourceGame/workshop.resource-prefab.json",
    "Common/Prefabs/ResourceGame/resource_node.resource-prefab.json"
)

$zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
try {
    $manifest = Read-ZipEntryText -Zip $zip -EntryName "manifest.json"
    if ($manifest -notmatch '"IncludesAssetPack"\s*:\s*true') {
        throw "manifest.json does not enable IncludesAssetPack"
    }

    $pageEntries = $zip.Entries | Where-Object { $_.FullName -like "Common/UI/Custom/Pages/*.html" } | Sort-Object FullName
    if (-not $pageEntries) {
        throw "No HyUI pages were packaged into the plugin jar"
    }

    foreach ($page in $requiredPages) {
        if (-not ($pageEntries.FullName -contains $page)) {
            throw "Required HyUI page is missing from the built jar: $page"
        }
    }

    foreach ($asset in $requiredGeneratedAssets) {
        if (-not ($zip.Entries.FullName -contains $asset)) {
            throw "Required generated asset is missing from the built jar: $asset"
        }
    }

    $pngAssetCount = @($zip.Entries | Where-Object { $_.FullName -like "Common/UI/Custom/Textures/ResourceGame/*.png" -or $_.FullName -like "Common/UI/Custom/Textures/ResourceGame/*/*.png" }).Count
    if ($pngAssetCount -lt 40) {
        throw "Generated UI asset count is too low: $pngAssetCount"
    }

    $modelAssetCount = @($zip.Entries | Where-Object { $_.FullName -match '^Common/Models/ResourceGame/.+\.bbmodel$' }).Count
    if ($modelAssetCount -lt 222) {
        throw "Generated Blockbench model count is too low: $modelAssetCount"
    }

    $prefabRecipeCount = @($zip.Entries | Where-Object { $_.FullName -match '^Common/Prefabs/ResourceGame/.+\.resource-prefab\.json$' }).Count
    if ($prefabRecipeCount -lt 222) {
        throw "Generated prefab recipe count is too low: $prefabRecipeCount"
    }

    $modelManifestText = Read-ZipEntryText -Zip $zip -EntryName "Common/Models/ResourceGame/resource-game-model-assets.json"
    $modelManifest = $modelManifestText | ConvertFrom-Json
    if ($modelManifest.schema -ne "resource-game-model-assets/v1") {
        throw "Model asset manifest has an unexpected schema: $($modelManifest.schema)"
    }
    if ($modelManifest.maxBuildingLevel -ne 30) {
        throw "Model asset manifest has an unexpected maxBuildingLevel: $($modelManifest.maxBuildingLevel)"
    }
    if (@($modelManifest.assets).Count -ne $modelAssetCount) {
        throw "Model asset manifest count does not match packaged Blockbench models: manifest=$(@($modelManifest.assets).Count) packaged=$modelAssetCount"
    }
    foreach ($assetNode in @($modelManifest.assets)) {
        $modelEntryName = $assetNode.model -replace '^src/main/resources/', ''
        $recipeEntryName = $assetNode.prefabRecipe -replace '^src/main/resources/', ''
        if (-not ($zip.Entries.FullName -contains $modelEntryName)) {
            throw "Model manifest references an unpackaged model: $modelEntryName"
        }
        if (-not ($zip.Entries.FullName -contains $recipeEntryName)) {
            throw "Model manifest references an unpackaged prefab recipe: $recipeEntryName"
        }
    }

    foreach ($entry in $pageEntries) {
        $content = Read-ZipEntryText -Zip $zip -EntryName $entry.FullName
        if ([string]::IsNullOrWhiteSpace($content)) {
            throw "HyUI page is empty: $($entry.FullName)"
        }
        if ($content[0] -eq [char]0xFEFF) {
            throw "HyUI page has UTF-8 BOM: $($entry.FullName)"
        }
        if (-not $content.Contains('<style>')) {
            throw "HyUI page is missing an inline style block: $($entry.FullName)"
        }
        if (-not $content.Contains('id="ResourceGamePageRoot"')) {
            throw "HyUI page is missing the Resource Game page root: $($entry.FullName)"
        }
        if ($content.Contains("../Common.ui")) {
            throw "HyUI page still imports Common.ui: $($entry.FullName)"
        }
        if ($content -match '<script\b') {
            throw "HyUI page contains unsupported script content: $($entry.FullName)"
        }
        if (-not $content.Contains("../Textures/ResourceGame/")) {
            throw "HyUI page is not using generated ResourceGame textures: $($entry.FullName)"
        }
        if (-not $content.Contains("<img")) {
            throw "HyUI page is not using a generated icon image: $($entry.FullName)"
        }
        if ($content.Contains('src="../Textures/ResourceGame/')) {
            throw "HyUI page uses a native .ui-relative img src path that HyUI AssetImage cannot resolve: $($entry.FullName)"
        }
        if (-not $content.Contains('src="Textures/ResourceGame/')) {
            throw "HyUI page is not using a HyUI AssetImage-compatible generated icon path: $($entry.FullName)"
        }
        if (-not $content.Contains("buttons/")) {
            throw "HyUI page is not using generated button assets: $($entry.FullName)"
        }
        if (-not $content.Contains("<button")) {
            throw "HyUI page has no interactive button elements: $($entry.FullName)"
        }
        foreach ($token in $unsafeTokens) {
            if ($content.Contains($token)) {
                throw "HyUI page uses unsafe token '$token': $($entry.FullName)"
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
    generatedAssetCount = $requiredGeneratedAssets.Count
    generatedUiPngCount = $pngAssetCount
    generatedModelCount = $modelAssetCount
    generatedPrefabRecipeCount = $prefabRecipeCount
    requiredPages = $requiredPages
    requiredGeneratedAssets = $requiredGeneratedAssets
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
