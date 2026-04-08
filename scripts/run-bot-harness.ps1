param(
    [string]$Version = "1.21.4",
    [string]$HarnessDir = "",
    [string]$LogDir = "",
    [switch]$InstallDeps
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($HarnessDir)) {
    $HarnessDir = [System.IO.Path]::GetFullPath((Join-Path $repoRoot "..\..\tavall-java-game-tools\minecraft-bot"))
}
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}

$scriptPath = Join-Path $HarnessDir "minecraft-smoke-bot-scripts\run-java-smoke.mjs"
if (-not (Test-Path $scriptPath)) {
    throw "Harness script not found at $scriptPath"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logPath = Join-Path $LogDir ("java-smoke-{0}-{1}.log" -f $Version.Replace(".", "_"), $timestamp)
$summaryPath = Join-Path $LogDir ("java-smoke-{0}-{1}.json" -f $Version.Replace(".", "_"), $timestamp)

function Write-LogLine {
    param([string]$Message)
    Add-Content -Path $logPath -Value $Message -Encoding utf8
    Write-Host $Message
}

function Invoke-LoggedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    $stdoutPath = [System.IO.Path]::GetTempFileName()
    $stderrPath = [System.IO.Path]::GetTempFileName()
    try {
        $process = Start-Process -FilePath $FilePath `
            -ArgumentList $Arguments `
            -Wait `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath

        foreach ($path in @($stdoutPath, $stderrPath)) {
            if (-not (Test-Path $path)) {
                continue
            }

            Get-Content -Path $path | ForEach-Object {
                Add-Content -Path $logPath -Value $_ -Encoding utf8
                Write-Host $_
            }
        }

        return [int]$process.ExitCode
    } finally {
        foreach ($path in @($stdoutPath, $stderrPath)) {
            if (Test-Path $path) {
                Remove-Item -Path $path -Force
            }
        }
    }
}

$startedAt = (Get-Date).ToString("o")
$serverReachable = $false
try {
    $serverReachable = (Test-NetConnection -ComputerName 127.0.0.1 -Port 25565 -WarningAction SilentlyContinue).TcpTestSucceeded
} catch {
    $serverReachable = $false
}

Write-LogLine ("[{0}] Starting repo-local bot harness run" -f $startedAt)
Write-LogLine ("[{0}] HarnessDir={1}" -f (Get-Date).ToString("o"), $HarnessDir)
Write-LogLine ("[{0}] Version={1}" -f (Get-Date).ToString("o"), $Version)
Write-LogLine ("[{0}] ServerReachable={1}" -f (Get-Date).ToString("o"), $serverReachable)

if ($InstallDeps -or -not (Test-Path (Join-Path $HarnessDir "node_modules"))) {
    Write-LogLine ("[{0}] Installing harness dependencies with npm ci" -f (Get-Date).ToString("o"))
    Push-Location $HarnessDir
    try {
        $npmExitCode = Invoke-LoggedCommand -FilePath "npm.cmd" -Arguments @("ci")
        if ($npmExitCode -ne 0) {
            throw "npm ci failed with exit code $npmExitCode"
        }
    } finally {
        Pop-Location
    }
}

$exitCode = 0
$completedAt = $null

Push-Location $HarnessDir
try {
    $exitCode = Invoke-LoggedCommand -FilePath "node.exe" -Arguments @($scriptPath, $Version)
} finally {
    $completedAt = (Get-Date).ToString("o")
    Pop-Location
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = $completedAt
    version = $Version
    harnessDir = $HarnessDir
    scriptPath = $scriptPath
    serverReachable = $serverReachable
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
}

$summary | ConvertTo-Json | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

if ($exitCode -ne 0) {
    exit $exitCode
}
