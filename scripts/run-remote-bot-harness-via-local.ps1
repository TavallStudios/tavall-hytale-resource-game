param(
    [string]$SshAlias = "novus-remote",
    [string]$SshConfigPath = "C:\\Users\\TJ\\.ssh\\config",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$RemoteServerLogDir = "/srv/hytale-startup-patch-test/Server/logs",
    [string]$Scenario = "connect-only",
    [string]$Username = "ResourceGameBot",
    [string]$BridgeHost = "127.0.0.1",
    [int]$BridgePort = 5520,
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 5520,
    [int]$RemoteForwardPort = 5521,
    [switch]$StartBridge,
    [string]$LocalServerRoot = "F:\\Games\\Hytale\\install\\release\\package\\game\\latest\\Server",
    [string]$LogDir = "",
    [string]$AuthDomain = "",
    [string]$IdentityToken = "",
    [string]$SessionToken = "",
    [string]$AuthPassword = "",
    [string]$AuthScopes = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "remote-via-local-{0}-{1}" -f $Scenario, $timestamp
$logPath = Join-Path $LogDir ($baseName + "-run.txt")
$summaryPath = Join-Path $LogDir ($baseName + "-summary.txt")
$resultPath = Join-Path $LogDir ($baseName + "-scenario-result.txt")

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
            -ArgumentList @("-F", $SshConfigPath, $SshAlias, $RemoteCommand) `
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
$remoteResultFile = "{0}/scenario-result.txt" -f $remoteOutputDir
$authArgs = ""
if ([string]::IsNullOrWhiteSpace($AuthDomain) -and -not [string]::IsNullOrWhiteSpace($env:HYTALE_AUTH_DOMAIN)) {
    $AuthDomain = $env:HYTALE_AUTH_DOMAIN
}
if (-not [string]::IsNullOrWhiteSpace($AuthDomain)) {
    $authArgs += " --auth-domain '$AuthDomain'"
}
if (-not [string]::IsNullOrWhiteSpace($IdentityToken)) {
    $authArgs += " --identity-token '$IdentityToken'"
}
if (-not [string]::IsNullOrWhiteSpace($SessionToken)) {
    $authArgs += " --session-token '$SessionToken'"
}
if (-not [string]::IsNullOrWhiteSpace($AuthPassword)) {
    $authArgs += " --auth-password '$AuthPassword'"
}
if (-not [string]::IsNullOrWhiteSpace($AuthScopes)) {
    $authArgs += " --auth-scopes '$AuthScopes'"
}

Write-LogLine ("[{0}] Starting remote bot harness (via local server)" -f $startedAt)
Write-LogLine ("[{0}] SSH alias={1}" -f (Get-Date).ToString("o"), $SshAlias)
Write-LogLine ("[{0}] Scenario={1}" -f (Get-Date).ToString("o"), $Scenario)
Write-LogLine ("[{0}] RemoteHarnessDir={1}" -f (Get-Date).ToString("o"), $RemoteHarnessDir)
Write-LogLine ("[{0}] LocalBridge={1}:{2} LocalServer={3}:{4}" -f (Get-Date).ToString("o"), $BridgeHost, $BridgePort, $ServerHost, $ServerPort)
Write-LogLine ("[{0}] RemoteForwardPort={1}" -f (Get-Date).ToString("o"), $RemoteForwardPort)

if ($StartBridge) {
    & (Join-Path $PSScriptRoot "run-local-quic-bridge.ps1") `
        -ServerRoot $LocalServerRoot `
        -BridgeHost $BridgeHost `
        -BridgePort $BridgePort `
        -ServerHost $ServerHost `
        -ServerPort $ServerPort
}

$tunnelArgs = @(
    "-F", $SshConfigPath,
    "-N",
    "-R", "{0}:127.0.0.1:{1}" -f $RemoteForwardPort, $BridgePort,
    $SshAlias
)

$completedAt = $null
$exitCode = 0
$tunnelProcess = $null
try {
    Write-LogLine ("[{0}] Opening reverse SSH tunnel" -f (Get-Date).ToString("o"))
    $tunnelProcess = Start-Process -FilePath "ssh.exe" -ArgumentList $tunnelArgs -PassThru
    Start-Sleep -Seconds 2

    $remoteCommand = @"
set -e
cd '$RemoteHarnessDir'
mkdir -p '$remoteOutputDir'
node ./apps/cli/dist/index.js scenario $Scenario --host 127.0.0.1 --port $RemoteForwardPort --username '$Username' --authoritative-log-dir '$RemoteServerLogDir' --output-dir '$remoteOutputDir' $authArgs
"@

    $exitCode = Invoke-SshCapture -RemoteCommand $remoteCommand
    $completedAt = (Get-Date).ToString("o")

    if ($exitCode -eq 0) {
        $scpOut = [System.IO.Path]::GetTempFileName()
        $scpErr = [System.IO.Path]::GetTempFileName()
        try {
            $scpProcess = Start-Process -FilePath "scp.exe" `
                -ArgumentList @("-F", $SshConfigPath, ("{0}:{1}" -f $SshAlias, $remoteResultFile), $resultPath) `
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
        } finally {
            foreach ($path in @($scpOut, $scpErr)) {
                if (Test-Path $path) {
                    Remove-Item -Path $path -Force
                }
            }
        }
    }
} finally {
    if ($tunnelProcess -and -not $tunnelProcess.HasExited) {
        Stop-Process -Id $tunnelProcess.Id -ErrorAction SilentlyContinue
    }
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = $completedAt
    sshAlias = $SshAlias
    remoteHarnessDir = $RemoteHarnessDir
    remoteServerLogDir = $RemoteServerLogDir
    scenario = $Scenario
    username = $Username
    localBridgeHost = $BridgeHost
    localBridgePort = $BridgePort
    localServerHost = $ServerHost
    localServerPort = $ServerPort
    remoteForwardPort = $RemoteForwardPort
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
    scenarioResultPath = $(if (Test-Path $resultPath) { $resultPath } else { $null })
}

Set-TextSummary -Path $summaryPath -Data $summary
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode
