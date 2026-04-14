param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-resource-game-flow.mjs",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5522,
    [string]$Username = "ResourceGameBot",
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
    -ServerPort $Port

$remoteCommand = "cd $RemoteHarnessDir && mkdir -p $remoteOutputDir && node $remoteScriptPath $ServerHost $Port $Username $remoteOutputDir"
$exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $SshAlias,
    $remoteCommand
)

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

Minimize-TranscriptArtifact -Path $tracePath

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    sshAlias = $SshAlias
    host = $ServerHost
    port = $Port
    username = $Username
    remoteHarnessDir = $RemoteHarnessDir
    remoteScriptPath = $remoteScriptPath
    remoteOutputDir = $remoteOutputDir
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
    scenarioResultPath = $(if (Test-Path $resultPath) { $resultPath } else { $null })
    transcriptPath = $(if (Test-Path $tracePath) { $tracePath } else { $null })
}

$summary | ConvertTo-Json | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode

