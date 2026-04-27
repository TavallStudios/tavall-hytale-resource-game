param(
    [string]$LogDir = "",
    [int]$MaxAttemptsPerStep = 2
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $LogDir ("remote-full-suite-{0}-summary.txt" -f $timestamp)
$startedAt = (Get-Date).ToString("o")

$mvn = "C:/Program Files/JetBrains/IntelliJ IDEA 2025.3/plugins/maven/lib/maven3/bin/mvn.cmd"
& $mvn -q test package
if ($LASTEXITCODE -ne 0) {
    throw "Local build failed."
}

powershell -ExecutionPolicy Bypass -File .\scripts\validate-custom-ui-assets.ps1
if ($LASTEXITCODE -ne 0) {
    throw "Custom UI asset-pack validation failed."
}

powershell -ExecutionPolicy Bypass -File .\scripts\install-hyui-remote.ps1
if ($LASTEXITCODE -ne 0) {
    throw "HyUI remote install failed."
}

powershell -ExecutionPolicy Bypass -File .\scripts\sync-remote-bot-harness.ps1
if ($LASTEXITCODE -ne 0) {
    throw "Remote bot harness sync failed."
}

$steps = @(
    @{ name = "persistence"; script = ".\scripts\run-remote-persistence-flow.ps1" },
    @{ name = "castle"; script = ".\scripts\run-remote-castle-interaction-flow.ps1" },
    @{ name = "resource"; script = ".\scripts\run-remote-resource-game-flow.ps1" },
    @{ name = "command-alias"; script = ".\scripts\run-remote-command-alias-flow.ps1" },
    @{ name = "data-health"; script = ".\scripts\run-remote-data-health-flow.ps1" },
    @{ name = "onboarding"; script = ".\scripts\run-remote-onboarding-flow.ps1" },
    @{ name = "interior-tour"; script = ".\scripts\run-remote-interior-tour-flow.ps1" },
    @{ name = "placement"; script = ".\scripts\run-remote-placement-flow.ps1" },
    @{ name = "building-upgrade"; script = ".\scripts\run-remote-building-upgrade-flow.ps1" },
    @{ name = "node-assignment"; script = ".\scripts\run-remote-node-assignment-flow.ps1" },
    @{ name = "ui-edge"; script = ".\scripts\run-remote-ui-edge-flow.ps1" },
    @{ name = "visual-counter"; script = ".\scripts\run-remote-visual-counter-flow.ps1" }
)

$results = @()
foreach ($step in $steps) {
    $passed = $false
    $attempts = @()
    for ($attempt = 1; $attempt -le $MaxAttemptsPerStep; $attempt++) {
        powershell -ExecutionPolicy Bypass -File $step.script
        if ($LASTEXITCODE -eq 0) {
            $passed = $true
            $attempts += [ordered]@{
                attempt = $attempt
                status = "passed"
            }
            break
        }
        $attempts += [ordered]@{
            attempt = $attempt
            status = "failed"
        }
        if ($attempt -lt $MaxAttemptsPerStep) {
            Start-Sleep -Seconds 3
        }
    }
    if (-not $passed) {
        throw "Remote suite step failed: $($step.name)"
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
    results = $results
}

Set-TextSummary -Path $summaryPath -Data $summary
Write-Host ("SummaryFile={0}" -f $summaryPath)
