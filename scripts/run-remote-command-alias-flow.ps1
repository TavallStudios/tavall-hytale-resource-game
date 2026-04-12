param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-command-alias-flow.mjs",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5520,
    [string]$Username = "AliasBot",
    [string]$StableUuid = "423e4567-e89b-12d3-a456-426614174000",
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
$baseName = "remote-command-alias-flow-{0}" -f $timestamp
$logPath = Join-Path $LogDir ($baseName + ".log")
$summaryPath = Join-Path $LogDir ($baseName + ".json")
$resultPath = Join-Path $LogDir ($baseName + "-scenario-result.json")
$tracePath = Join-Path $LogDir ($baseName + "-transcript.json")
$remoteScriptPath = "/tmp/{0}.mjs" -f $baseName
$remoteOutputDir = "/tmp/{0}" -f $baseName

function Write-LogLine {
    param([string]$Message)
    Add-Content -Path $logPath -Value $Message -Encoding utf8
    Write-Host $Message
}

function Invoke-ProcessCapture {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [switch]$AllowFailure
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

        if (-not $AllowFailure -and $process.ExitCode -ne 0) {
            throw "Command failed: $FilePath $($Arguments -join ' ')"
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
Write-LogLine ("[{0}] Starting remote command alias flow" -f $startedAt)
Write-LogLine ("[{0}] SSH alias={1}" -f (Get-Date).ToString("o"), $SshAlias)
Write-LogLine ("[{0}] Host={1} Port={2}" -f (Get-Date).ToString("o"), $ServerHost, $Port)
Write-LogLine ("[{0}] StableUuid={1}" -f (Get-Date).ToString("o"), $StableUuid)

Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $ScenarioScriptPath,
    ("{0}:{1}" -f $SshAlias, $remoteScriptPath)
) | Out-Null

Ensure-RemoteQuicBridge `
    -SshAlias $SshAlias `
    -BridgeSourcePath $bridgeSourcePath `
    -LogPath $logPath `
    -BridgePort $Port `
    -ServerHost $ServerHost `
    -ServerPort $Port

$remoteCommand = "cd $RemoteHarnessDir && mkdir -p $remoteOutputDir && node $remoteScriptPath $ServerHost $Port $Username $StableUuid $remoteOutputDir"
$exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $SshAlias,
    $remoteCommand
) -AllowFailure
if ($exitCode -ne 0) {
    throw "Remote command alias scenario failed"
}

Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    ("{0}:{1}/scenario-result.json" -f $SshAlias, $remoteOutputDir),
    $resultPath
) | Out-Null
Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    ("{0}:{1}/transcript.json" -f $SshAlias, $remoteOutputDir),
    $tracePath
) | Out-Null

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    sshAlias = $SshAlias
    host = $ServerHost
    port = $Port
    username = $Username
    stableUuid = $StableUuid
    success = $true
    logPath = $logPath
    scenarioResultPath = $resultPath
    transcriptPath = $tracePath
}

$summary | ConvertTo-Json -Depth 5 | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] Command alias flow passed" -f (Get-Date).ToString("o"))
