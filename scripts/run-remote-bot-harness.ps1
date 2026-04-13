param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$RemoteServerLogDir = "/srv/hytale-startup-patch-test/Server/logs",
    [string]$Scenario = "connect-only",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5522,
    [string]$Username = "ResourceGameBot",
    [string]$ServerRoot = "/srv/hytale-startup-patch-test",
    [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "remote-quic-harness.ps1")

$repoRoot = Split-Path -Parent $PSScriptRoot
$bridgeSourcePath = Join-Path $PSScriptRoot "HytaleQuicTcpBridge.java"
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "remote-{0}-{1}" -f $Scenario, $timestamp
$logPath = Join-Path $LogDir ($baseName + ".log")
$summaryPath = Join-Path $LogDir ($baseName + ".json")
$resultPath = Join-Path $LogDir ($baseName + "-scenario-result.json")

function Write-LogLine {
    param([string]$Message)
    Add-Content -Path $logPath -Value $Message -Encoding utf8
    Write-Host $Message
}

function Invoke-SshCapture {
    param([string]$RemoteCommand)

    $tempOut = [System.IO.Path]::GetTempFileName()
    $tempErr = [System.IO.Path]::GetTempFileName()
    try {
        $process = Start-Process -FilePath "ssh.exe" `
            -ArgumentList @("-F", "C:\Users\TJ\.ssh\config", $SshAlias, $RemoteCommand) `
            -Wait `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $tempOut `
            -RedirectStandardError $tempErr

        foreach ($path in @($tempOut, $tempErr)) {
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
        foreach ($path in @($tempOut, $tempErr)) {
            if (Test-Path $path) {
                Remove-Item -Path $path -Force
            }
        }
    }
}

$startedAt = (Get-Date).ToString("o")
$remoteOutputDir = "/tmp/{0}" -f $baseName
$remoteResultFile = "{0}/scenario-result.json" -f $remoteOutputDir

Write-LogLine ("[{0}] Starting remote bot harness run" -f $startedAt)
Write-LogLine ("[{0}] SSH alias={1}" -f (Get-Date).ToString("o"), $SshAlias)
Write-LogLine ("[{0}] Scenario={1}" -f (Get-Date).ToString("o"), $Scenario)
Write-LogLine ("[{0}] RemoteHarnessDir={1}" -f (Get-Date).ToString("o"), $RemoteHarnessDir)
Write-LogLine ("[{0}] Host={1} Port={2}" -f (Get-Date).ToString("o"), $ServerHost, $Port)

Ensure-RemoteQuicBridge `
    -SshAlias $SshAlias `
    -BridgeSourcePath $bridgeSourcePath `
    -LogPath $logPath `
    -BridgePort $Port `
    -ServerHost $ServerHost `
    -ServerPort $Port `
    -ServerRoot $ServerRoot

$remoteCommand = @"
set -e
cd '$RemoteHarnessDir'
mkdir -p '$remoteOutputDir'
node ./apps/cli/dist/index.js scenario $Scenario --host $ServerHost --port $Port --username '$Username' --authoritative-log-dir '$RemoteServerLogDir' --output-dir '$remoteOutputDir' --json
"@

$exitCode = Invoke-SshCapture -RemoteCommand $remoteCommand
$completedAt = (Get-Date).ToString("o")

if ($exitCode -eq 0) {
    $scpOut = [System.IO.Path]::GetTempFileName()
    $scpErr = [System.IO.Path]::GetTempFileName()
    try {
        $scpProcess = Start-Process -FilePath "scp.exe" `
            -ArgumentList @("-F", "C:\Users\TJ\.ssh\config", ("{0}:{1}" -f $SshAlias, $remoteResultFile), $resultPath) `
            -Wait `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $scpOut `
            -RedirectStandardError $scpErr

        foreach ($path in @($scpOut, $scpErr)) {
            if (-not (Test-Path $path)) {
                continue
            }

            Get-Content -Path $path | ForEach-Object {
                Add-Content -Path $logPath -Value $_ -Encoding utf8
                Write-Host $_
            }
        }

        if ($scpProcess.ExitCode -ne 0) {
            Write-LogLine ("[{0}] scp failed with exit code {1}" -f (Get-Date).ToString("o"), $scpProcess.ExitCode)
        }
    } finally {
        foreach ($path in @($scpOut, $scpErr)) {
            if (Test-Path $path) {
                Remove-Item -Path $path -Force
            }
        }
    }
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = $completedAt
    sshAlias = $SshAlias
    remoteHarnessDir = $RemoteHarnessDir
    remoteServerLogDir = $RemoteServerLogDir
    scenario = $Scenario
    host = $ServerHost
    port = $Port
    username = $Username
    remoteOutputDir = $remoteOutputDir
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
    scenarioResultPath = $(if (Test-Path $resultPath) { $resultPath } else { $null })
}

$summary | ConvertTo-Json | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode
