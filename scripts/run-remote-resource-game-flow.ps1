param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-resource-game-flow.mjs",
    [string]$ServerRoot = "/srv/hytale-startup-patch-test",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5522,
    [string]$Username = "ResourceGameBot",
    [string]$StableUuid = "523e4567-e89b-12d3-a456-426614174000",
    [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "remote-quic-harness.ps1")

$repoRoot = Split-Path -Parent $PSScriptRoot
$bridgeSourcePath = Join-Path $PSScriptRoot "HytaleQuicTcpBridge.java"
$helperScriptPath = Join-Path $PSScriptRoot "bot-flow-helpers.mjs"
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "remote-resource-game-flow-{0}" -f $timestamp
$logPath = Join-Path $LogDir ($baseName + "-run.txt")
$summaryPath = Join-Path $LogDir ($baseName + "-summary.txt")
$resultPath = Join-Path $LogDir ($baseName + "-scenario-result.txt")
$tracePath = Join-Path $LogDir ($baseName + "-transcript.txt")
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
Write-LogLine ("[{0}] Starting remote resource game flow" -f $startedAt)
Write-LogLine ("[{0}] SSH alias={1}" -f (Get-Date).ToString("o"), $SshAlias)
Write-LogLine ("[{0}] Host={1} Port={2}" -f (Get-Date).ToString("o"), $ServerHost, $Port)
Write-LogLine ("[{0}] StableUuid={1}" -f (Get-Date).ToString("o"), $StableUuid)

$copyScriptExit = Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $ScenarioScriptPath,
    ("{0}:{1}" -f $SshAlias, $remoteScriptPath)
)
if ($copyScriptExit -ne 0) {
    throw "Failed to copy remote scenario script"
}
Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $helperScriptPath,
    ("{0}:/tmp/bot-flow-helpers.mjs" -f $SshAlias)
) | Out-Null


Ensure-RemoteQuicBridge `
    -SshAlias $SshAlias `
    -BridgeSourcePath $bridgeSourcePath `
    -LogPath $logPath `
    -BridgePort $Port `
    -ServerHost $ServerHost `
    -ServerPort $Port `
    -ServerRoot $ServerRoot

$remoteCommand = "cd $RemoteHarnessDir && export HYTALE_SERVER_JAR=$ServerRoot/Server/HytaleServer.jar && unset HYTALE_AUTH_DOMAIN HYTALE_IDENTITY_TOKEN HYTALE_SESSION_TOKEN HYTALE_AUTH_PASSWORD HYTALE_AUTH_SCOPES && mkdir -p $remoteOutputDir && node $remoteScriptPath $ServerHost $Port $Username $StableUuid $remoteOutputDir"
$exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $SshAlias,
    $remoteCommand
)

Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    ("{0}:{1}/scenario-result.txt" -f $SshAlias, $remoteOutputDir),
    $resultPath
) | Out-Null

Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    ("{0}:{1}/transcript.txt" -f $SshAlias, $remoteOutputDir),
    $tracePath
) | Out-Null

Minimize-TranscriptArtifact -Path $tracePath

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    sshAlias = $SshAlias
    host = $ServerHost
    port = $Port
    username = $Username
    stableUuid = $StableUuid
    remoteHarnessDir = $RemoteHarnessDir
    remoteScriptPath = $remoteScriptPath
    remoteOutputDir = $remoteOutputDir
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
    scenarioResultPath = $(if (Test-Path $resultPath) { $resultPath } else { $null })
    transcriptPath = $(if (Test-Path $tracePath) { $tracePath } else { $null })
}

Set-TextSummary -Path $summaryPath -Data $summary
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode
