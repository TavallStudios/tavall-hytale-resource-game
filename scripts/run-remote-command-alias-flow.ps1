param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-command-alias-flow.mjs",
    [string]$PluginJarPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/target/tavall-hytale-resource-game.jar",
    [string]$RemotePluginJarPath = "/srv/hytale-startup-patch-test/Server/mods/tavall-hytale-resource-game.jar",
    [string]$ServerRoot = "/srv/hytale-startup-patch-test",
    [string]$Transport = "QUIC",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5522,
    [string]$Username = "AliasBot",
    [string]$StableUuid = "423e4567-e89b-12d3-a456-426614174000",
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
$baseName = "remote-command-alias-flow-{0}" -f $timestamp
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

function Restart-RemoteServer {
    $script = @'
set -e
pid=$(lsof -ti :{0} || true)
if [ -n "$pid" ]; then
  kill $pid || true
  sleep 2
fi
cd {1}
rm -f {1}/Server/universe/players/{3}.json {1}/Server/universe/players/{3}.json.bak
unset TAVALL_POSTGRES_URL TAVALL_POSTGRES_USER TAVALL_POSTGRES_PASSWORD
unset TAVALL_REDIS_HOST TAVALL_REDIS_PORT TAVALL_REDIS_PASSWORD TAVALL_REDIS_TLS
nohup ./start.sh --transport {2} --auth-mode OFFLINE --allow-op --bind 0.0.0.0:{0} > start.out 2>&1 < /dev/null &
'@ -f $Port, $ServerRoot, $Transport, $StableUuid
    Invoke-RemoteLoggedBash -SshAlias $SshAlias -Script $script -LogPath $logPath | Out-Null
    Wait-RemoteServerReady -SshAlias $SshAlias -LogPath $logPath -Transport $Transport -Port $Port -StartOutPath "$ServerRoot/start.out"
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
Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $helperScriptPath,
    ("{0}:/tmp/bot-flow-helpers.mjs" -f $SshAlias)
) | Out-Null
Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $PluginJarPath,
    ("{0}:{1}" -f $SshAlias, $RemotePluginJarPath)
) | Out-Null

$remoteModsDir = $RemotePluginJarPath -replace "/[^/]+$", ""
powershell -ExecutionPolicy Bypass -File .\scripts\install-hyui-remote.ps1 -SshAlias $SshAlias -RemoteModsDir $remoteModsDir | Out-Null

Restart-RemoteServer
Ensure-RemoteQuicBridge `
    -SshAlias $SshAlias `
    -BridgeSourcePath $bridgeSourcePath `
    -LogPath $logPath `
    -BridgePort $Port `
    -ServerHost $ServerHost `
    -ServerPort $Port `
    -ServerRoot $ServerRoot
Start-Sleep -Seconds 2

$botTraceMode = $env:RESOURCE_GAME_BOT_TRACE
$botOutputMode = $env:RESOURCE_GAME_BOT_OUTPUT
$remoteEnvironment = "export HYTALE_SERVER_JAR=$ServerRoot/Server/HytaleServer.jar"
if (-not [string]::IsNullOrWhiteSpace($botTraceMode)) {
    $remoteEnvironment += " RESOURCE_GAME_BOT_TRACE=$botTraceMode"
}
if (-not [string]::IsNullOrWhiteSpace($botOutputMode)) {
    $remoteEnvironment += " RESOURCE_GAME_BOT_OUTPUT=$botOutputMode"
}
$remoteCommand = "cd $RemoteHarnessDir && $remoteEnvironment && unset HYTALE_AUTH_DOMAIN HYTALE_IDENTITY_TOKEN HYTALE_SESSION_TOKEN HYTALE_AUTH_PASSWORD HYTALE_AUTH_SCOPES && mkdir -p $remoteOutputDir && node $remoteScriptPath $ServerHost $Port $Username $StableUuid $remoteOutputDir"
$exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $SshAlias,
    $remoteCommand
) -AllowFailure
if ($exitCode -ne 0) {
    Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
        "-F", "C:\Users\TJ\.ssh\config",
        ("{0}:{1}/scenario-result.txt" -f $SshAlias, $remoteOutputDir),
        $resultPath
    ) -AllowFailure | Out-Null
    Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
        "-F", "C:\Users\TJ\.ssh\config",
        ("{0}:{1}/transcript.txt" -f $SshAlias, $remoteOutputDir),
        $tracePath
    ) -AllowFailure | Out-Null
    Minimize-TranscriptArtifact -Path $tracePath
    throw "Remote command alias scenario failed"
}

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
    success = $true
    logPath = $logPath
    scenarioResultPath = $resultPath
    transcriptPath = $tracePath
}

Set-TextSummary -Path $summaryPath -Data $summary
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] Command alias flow passed" -f (Get-Date).ToString("o"))
