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
        $name -like "*-transcript.json" -or
        $name -like "*-seed-transcript.json" -or
        $name -like "*-verify-transcript.json" -or
        $name -like "*-server-log.txt" -or
        ($Aggressive -and $name -like "*-scenario-result.json")

    if ($shouldRemove) {
        Remove-Item -LiteralPath $_.FullName -Force
        $removed += $_.FullName
    }
}

$summary = [ordered]@{
    logDir = $resolved
    aggressive = [bool]$Aggressive
    removedCount = $removed.Count
    removed = $removed
}

$summary | ConvertTo-Json -Depth 4
