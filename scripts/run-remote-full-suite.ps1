param(
    [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $LogDir ("remote-full-suite-{0}.json" -f $timestamp)
$startedAt = (Get-Date).ToString("o")

$mvn = "C:/Program Files/JetBrains/IntelliJ IDEA 2025.3/plugins/maven/lib/maven3/bin/mvn.cmd"
& $mvn -q test package
if ($LASTEXITCODE -ne 0) {
    throw "Local build failed."
}

$steps = @(
    @{ name = "persistence"; script = ".\scripts\run-remote-persistence-flow.ps1" },
    @{ name = "castle"; script = ".\scripts\run-remote-castle-interaction-flow.ps1" },
    @{ name = "resource"; script = ".\scripts\run-remote-resource-game-flow.ps1" },
    @{ name = "command-alias"; script = ".\scripts\run-remote-command-alias-flow.ps1" },
    @{ name = "data-health"; script = ".\scripts\run-remote-data-health-flow.ps1" },
    @{ name = "onboarding"; script = ".\scripts\run-remote-onboarding-flow.ps1" },
    @{ name = "interior-tour"; script = ".\scripts\run-remote-interior-tour-flow.ps1" },
    @{ name = "ui-edge"; script = ".\scripts\run-remote-ui-edge-flow.ps1" },
    @{ name = "visual-counter"; script = ".\scripts\run-remote-visual-counter-flow.ps1" }
)

$results = @()
foreach ($step in $steps) {
    powershell -ExecutionPolicy Bypass -File $step.script
    if ($LASTEXITCODE -ne 0) {
        throw "Remote suite step failed: $($step.name)"
    }
    powershell -ExecutionPolicy Bypass -File .\scripts\prune-bot-logs.ps1 -LogDir $LogDir | Out-Null
    $results += [ordered]@{
        name = $step.name
        status = "passed"
    }
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    success = $true
    results = $results
}

$summary | ConvertTo-Json -Depth 5 | Set-Content -Path $summaryPath -Encoding utf8
Write-Host ("SummaryFile={0}" -f $summaryPath)
