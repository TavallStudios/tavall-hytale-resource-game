param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-ui-edge-flow.mjs",
    [string]$PluginJarPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/target/tavall-hytale-resource-game.jar",
    [string]$RemotePluginJarPath = "/srv/hytale/Server/mods/tavall-hytale-resource-game.jar",
    [string]$ServerRoot = "/srv/hytale",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5520,
    [string]$Username = "UiEdgeBot",
    [string]$StableUuid = "323e4567-e89b-12d3-a456-426614174000",
    [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "remote-ui-edge-flow-{0}" -f $timestamp
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

function Invoke-RemoteBash {
    param([string]$Script)

    $tempScript = Join-Path $env:TEMP ([System.IO.Path]::GetRandomFileName() + ".sh")
    try {
        $normalizedScript = $Script -replace "`r`n", "`n"
        [System.IO.File]::WriteAllText($tempScript, $normalizedScript, (New-Object System.Text.UTF8Encoding($false)))

        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "ssh.exe"
        $processInfo.Arguments = "-F `"C:\Users\TJ\.ssh\config`" $SshAlias `"bash -s`""
        $processInfo.RedirectStandardInput = $true
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $null = $process.Start()
        $process.StandardInput.Write((Get-Content -Raw -Path $tempScript))
        $process.StandardInput.Close()
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()

        foreach ($line in ($stdout -split "`r?`n")) {
            if ($line -ne "") {
                Add-Content -Path $logPath -Value $line -Encoding utf8
                Write-Host $line
            }
        }
        foreach ($line in ($stderr -split "`r?`n")) {
            if ($line -ne "") {
                Add-Content -Path $logPath -Value $line -Encoding utf8
                Write-Host $line
            }
        }

        if ($process.ExitCode -ne 0) {
            throw "Remote script failed with exit code $($process.ExitCode)"
        }
        return $stdout.Trim()
    } finally {
        if (Test-Path $tempScript) {
            Remove-Item -Path $tempScript -Force
        }
    }
}

function Restart-RemoteServer {
    $script = @'
set -e
pid=$(lsof -ti tcp:{0} || true)
if [ -n "$pid" ]; then
  kill $pid || true
  sleep 2
fi
cd {1}
unset TAVALL_POSTGRES_URL TAVALL_POSTGRES_USER TAVALL_POSTGRES_PASSWORD
unset TAVALL_REDIS_HOST TAVALL_REDIS_PORT TAVALL_REDIS_PASSWORD TAVALL_REDIS_TLS
nohup ./start.sh --transport TCP --auth-mode INSECURE --allow-op > start.out 2>&1 < /dev/null &
for i in $(seq 1 60); do
  if lsof -ti tcp:{0} >/dev/null 2>&1; then
    echo SERVER_READY
    exit 0
  fi
  sleep 2
done
exit 1
'@ -f $Port, $ServerRoot
    Invoke-RemoteBash -Script $script | Out-Null
}

$startedAt = (Get-Date).ToString("o")
Write-LogLine ("[{0}] Starting remote UI edge flow" -f $startedAt)
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
    $PluginJarPath,
    ("{0}:{1}" -f $SshAlias, $RemotePluginJarPath)
) | Out-Null

Restart-RemoteServer
Start-Sleep -Seconds 2

$remoteCommand = "cd $RemoteHarnessDir && mkdir -p $remoteOutputDir && node $remoteScriptPath $ServerHost $Port $Username $StableUuid $remoteOutputDir"
$exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $SshAlias,
    $remoteCommand
) -AllowFailure
if ($exitCode -ne 0) {
    throw "Remote UI edge scenario failed"
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
Write-LogLine ("[{0}] UI edge flow passed" -f (Get-Date).ToString("o"))
