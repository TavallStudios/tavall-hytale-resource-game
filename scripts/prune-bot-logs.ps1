param(
    [string]$LogDir = "",
    [switch]$Aggressive
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}

$resolved = (Resolve-Path $LogDir).Path
if ($resolved -ne $LogDir) {
    throw "Unexpected bot log path: $resolved"
}

$removed = @()

Get-ChildItem -LiteralPath $resolved -Force | ForEach-Object {
    if ($_.Name -eq ".gitignore") {
        return
    }

    if ($_.PSIsContainer) {
        Remove-Item -LiteralPath $_.FullName -Recurse -Force
        $removed += $_.FullName
        return
    }

    $name = $_.Name
    $shouldRemove =
        $name -like "*.json" -or
        $name -like "*.log" -or
        $name -like "*-transcript.txt" -or
        $name -like "*-seed-transcript.txt" -or
        $name -like "*-verify-transcript.txt" -or
        $name -like "*-server-log.txt" -or
        ($Aggressive -and (
            $name -like "*-scenario-result.txt" -or
            $name -like "*.txt.summary"
        ))

    if ($shouldRemove) {
        Remove-Item -LiteralPath $_.FullName -Force
        $removed += $_.FullName
    }
}

$summaryLines = @(
    "logDir=$resolved",
    "aggressive=$([bool]$Aggressive)",
    "removedCount=$($removed.Count)"
)
if ($removed.Count -gt 0) {
    $summaryLines += ""
    $summaryLines += "[removed]"
    $summaryLines += $removed
}

$summaryLines -join [Environment]::NewLine
