param(
    [string]$ServerRoot = "F:\\Games\\Hytale\\install\\release\\package\\game\\latest\\Server",
    [string]$BridgeHost = "127.0.0.1",
    [int]$BridgePort = 5520,
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 5520,
    [string]$JavaPath = "java.exe",
    [string]$PidPath = "",
    [string]$BridgeDir = "",
    [switch]$Stop
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($PidPath)) {
    $PidPath = Join-Path $repoRoot "bot-logs\\local-quic-bridge.pid"
}
if ([string]::IsNullOrWhiteSpace($BridgeDir)) {
    $BridgeDir = Join-Path $env:TEMP "hytale-quic-bridge"
}

if ($Stop) {
    if (Test-Path $PidPath) {
        $bridgePid = Get-Content -Path $PidPath -ErrorAction SilentlyContinue
        if ($bridgePid) {
            Stop-Process -Id $bridgePid -ErrorAction SilentlyContinue
        }
        Remove-Item -Path $PidPath -Force
        Write-Host "Stopped local QUIC bridge."
    } else {
        Write-Host "No local QUIC bridge pid file found."
    }
    exit 0
}

$bridgeSourcePath = Join-Path $PSScriptRoot "HytaleQuicTcpBridge.java"
if (-not (Test-Path $bridgeSourcePath)) {
    throw "Bridge source not found at $bridgeSourcePath"
}

$serverJar = Join-Path $ServerRoot "HytaleServer.jar"
if (-not (Test-Path $serverJar)) {
    throw "HytaleServer.jar not found at $serverJar"
}

New-Item -ItemType Directory -Force -Path $BridgeDir | Out-Null
Get-ChildItem -Path $BridgeDir -Filter "*.class" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

Write-Host "Compiling local QUIC bridge..."
$javacArgs = @("-cp", $serverJar, "-d", $BridgeDir, $bridgeSourcePath)
$javacExitCode = (Start-Process -FilePath "javac.exe" -ArgumentList $javacArgs -Wait -NoNewWindow -PassThru).ExitCode
if ($javacExitCode -ne 0) {
    throw "javac failed with exit code $javacExitCode"
}

$classpath = "$serverJar;$BridgeDir"
$javaArgs = @("-cp", $classpath, "HytaleQuicTcpBridge", $BridgeHost, $BridgePort.ToString(), $ServerHost, $ServerPort.ToString())

Write-Host "Starting local QUIC bridge..."
$process = Start-Process -FilePath $JavaPath -ArgumentList $javaArgs -PassThru
$process.Id | Set-Content -Path $PidPath

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    $conn = Test-NetConnection -ComputerName $BridgeHost -Port $BridgePort -WarningAction SilentlyContinue
    if ($conn.TcpTestSucceeded) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $ready) {
    Write-Host "Bridge did not open TCP port $BridgePort yet."
} else {
    Write-Host ("Bridge ready on {0}:{1} (PID {2})." -f $BridgeHost, $BridgePort, $process.Id)
}
