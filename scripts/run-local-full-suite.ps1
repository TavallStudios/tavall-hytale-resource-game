param(
    [string]$LogDir = "",
    [int]$MaxAttemptsPerStep = 2,
    [string]$ServerHost = "127.0.0.1",
    [string]$AuthDomain = "auth.sanasol.ws",
    [int]$Port = 5520
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "remote-quic-harness.ps1")

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $LogDir ("local-full-suite-{0}-summary.txt" -f $timestamp)
$startedAt = (Get-Date).ToString("o")
$persistenceUuid = [guid]::NewGuid().ToString()
$castleUuid = [guid]::NewGuid().ToString()
$resourceUuid = [guid]::NewGuid().ToString()
$aliasUuid = [guid]::NewGuid().ToString()
$dataHealthUuid = [guid]::NewGuid().ToString()
$onboardingUuid = [guid]::NewGuid().ToString()
$interiorTourUuid = [guid]::NewGuid().ToString()
$interiorCycleUuid = [guid]::NewGuid().ToString()
$placementUuid = [guid]::NewGuid().ToString()
$buildingUpgradeUuid = [guid]::NewGuid().ToString()
$nodeAssignmentUuid = [guid]::NewGuid().ToString()
$uiEdgeUuid = [guid]::NewGuid().ToString()
$visualCounterUuid = [guid]::NewGuid().ToString()

$jarPath = Join-Path $repoRoot "target\tavall-hytale-resource-game.jar"
if (Test-Path $jarPath) {
    powershell -ExecutionPolicy Bypass -File .\scripts\validate-custom-ui-assets.ps1 -RepoRoot $repoRoot -JarPath $jarPath | Out-Null
}

$udpListener = Get-NetUDPEndpoint -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $Port }
if (-not $udpListener) {
    throw "Local Hytale server is not listening on UDP $Port. Run scripts/restart-local-dev-server.ps1 first."
}

$steps = @(
    @{ name = "persistence-seed"; kind = "scenario"; script = "scripts/remote-persistence-flow.mjs"; leadingArg = "seed"; username = "PersistenceBot"; stableUuid = $persistenceUuid },
    @{ name = "server-restart"; kind = "restart" },
    @{ name = "persistence-verify"; kind = "scenario"; script = "scripts/remote-persistence-flow.mjs"; leadingArg = "verify"; username = "PersistenceBot"; stableUuid = $persistenceUuid },
    @{ name = "castle"; script = "scripts/remote-castle-interaction-flow.mjs"; username = "CastleBot"; stableUuid = $castleUuid },
    @{ name = "resource"; script = "scripts/remote-resource-game-flow.mjs"; username = "ResourceBot"; stableUuid = $resourceUuid },
    @{ name = "command-alias"; script = "scripts/remote-command-alias-flow.mjs"; username = "AliasBot"; stableUuid = $aliasUuid },
    @{ name = "data-health"; script = "scripts/remote-data-health-flow.mjs"; username = "HealthBot"; stableUuid = $dataHealthUuid },
    @{ name = "onboarding"; script = "scripts/remote-onboarding-flow.mjs"; username = "OnboardingBot"; stableUuid = $onboardingUuid },
    @{ name = "interior-tour"; script = "scripts/remote-interior-tour-flow.mjs"; username = "InteriorTourBot"; stableUuid = $interiorTourUuid },
    @{ name = "interior-cycle"; script = "scripts/remote-interior-cycle-flow.mjs"; username = "InteriorCycleBot"; stableUuid = $interiorCycleUuid },
    @{ name = "placement"; script = "scripts/remote-placement-flow.mjs"; username = "PlacementBot"; stableUuid = $placementUuid },
    @{ name = "pre-building-upgrade-restart"; kind = "restart" },
    @{ name = "building-upgrade"; script = "scripts/remote-building-upgrade-flow.mjs"; username = "UpgradeBot"; stableUuid = $buildingUpgradeUuid },
    @{ name = "node-assignment"; script = "scripts/remote-node-assignment-flow.mjs"; username = "NodeBot"; stableUuid = $nodeAssignmentUuid },
    @{ name = "ui-edge"; script = "scripts/remote-ui-edge-flow.mjs"; username = "UiEdgeBot"; stableUuid = $uiEdgeUuid },
    @{ name = "visual-counter"; script = "scripts/remote-visual-counter-flow.mjs"; username = "VisualBot"; stableUuid = $visualCounterUuid }
)

$results = @()
foreach ($step in $steps) {
    $kind = $step.kind
    if ([string]::IsNullOrWhiteSpace($kind)) {
        $kind = "scenario"
    }
    if ($kind -eq "restart") {
        Write-Host ("[{0}] Step={1} restarting local server" -f (Get-Date).ToString("o"), $step.name)
        powershell -ExecutionPolicy Bypass -File .\scripts\restart-local-dev-server.ps1 -Port $Port -StartupTimeoutSeconds 240 | Out-Null
        $results += [ordered]@{ name = $step.name; status = "passed"; attempts = @([ordered]@{ attempt = 1; status = "passed" }) }
        continue
    }
    Write-Host ("[{0}] Step={1} username={2} uuid={3}" -f (Get-Date).ToString("o"), $step.name, $step.username, $step.stableUuid)
    $passed = $false
    $attempts = @()
    for ($attempt = 1; $attempt -le $MaxAttemptsPerStep; $attempt++) {
        $effectiveUuid = $step.stableUuid
        if ($attempt -gt 1 -and -not ($step.name -like "persistence-*")) {
            Write-Host ("[{0}] Step={1} restarting local server before retry {2}" -f (Get-Date).ToString("o"), $step.name, $attempt)
            powershell -ExecutionPolicy Bypass -File .\scripts\restart-local-dev-server.ps1 -Port $Port -StartupTimeoutSeconds 240 | Out-Null
            $effectiveUuid = [guid]::NewGuid().ToString()
        }
        $leadingArg = ""
        if ($step.ContainsKey("leadingArg") -and -not [string]::IsNullOrWhiteSpace($step.leadingArg)) {
            $leadingArg = $step.leadingArg
        }
        $invokeArgs = @(
            "-ExecutionPolicy", "Bypass",
            "-File", ".\\scripts\\run-local-gameplay-flow.ps1",
            "-ScenarioScript", $step.script,
            "-ScenarioName", $step.name,
            "-ServerHost", $ServerHost,
            "-Port", $Port,
            "-Username", $step.username,
            "-StableUuid", $effectiveUuid,
            "-AuthDomain", $AuthDomain
        )
        if (-not [string]::IsNullOrWhiteSpace($leadingArg)) {
            $invokeArgs += @("-LeadingArg", $leadingArg)
        }

        powershell @invokeArgs

        if ($LASTEXITCODE -eq 0) {
            $passed = $true
            $attempts += [ordered]@{ attempt = $attempt; status = "passed" }
            break
        }
        $attempts += [ordered]@{ attempt = $attempt; status = "failed" }
        if ($attempt -lt $MaxAttemptsPerStep) {
            Start-Sleep -Seconds 3
        }
    }
    if (-not $passed) {
        throw "Local suite step failed: $($step.name)"
    }
    powershell -ExecutionPolicy Bypass -File .\scripts\prune-bot-logs.ps1 -LogDir $LogDir | Out-Null
    $results += [ordered]@{
        name = $step.name
        status = "passed"
        attempts = $attempts
    }
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    success = $true
    host = $ServerHost
    port = $Port
    results = $results
}

Set-TextSummary -Path $summaryPath -Data $summary
Write-Host ("SummaryFile={0}" -f $summaryPath)
