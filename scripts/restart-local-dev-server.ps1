param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [switch]$BuildPlugin,
    [switch]$DeployPlugin = $true,
    [int]$Port = 5520,
    [int]$StartupTimeoutSeconds = 90
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$startScript = Join-Path $ServerRoot "start.bat"
if (-not (Test-Path $startScript)) {
    throw "start.bat not found at $startScript"
}

if ($DeployPlugin) {
    $deployScript = Join-Path $PSScriptRoot "deploy-local-plugin.ps1"
    & $deployScript -ServerRoot $ServerRoot -Build:$BuildPlugin
}

$serverProcesses = Get-CimInstance Win32_Process |
    Where-Object { $_.Name -match "^java(\\.exe)?$" -and $_.CommandLine -match "HytaleServer.jar" }

$launcherProcesses = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "cmd.exe" -and
        $_.CommandLine -like "*HyTaleDevServer*" -and
        $_.CommandLine -like "*start.bat*"
    }

$allProcessIds = @(
    $serverProcesses | Select-Object -ExpandProperty ProcessId
    $launcherProcesses | Select-Object -ExpandProperty ProcessId
) | Sort-Object -Unique

foreach ($processId in $allProcessIds) {
    try {
        & taskkill.exe /F /T /PID $processId | Out-Null
    } catch {
        Write-Warning ("Failed to stop local Hytale process tree {0}: {1}" -f $processId, $_.Exception.Message)
    }
}

$deadline = (Get-Date).AddSeconds(15)
do {
    $listener = Get-NetUDPEndpoint -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $Port }
    $remainingProcesses = Get-CimInstance Win32_Process |
        Where-Object {
            ($_.Name -match "^java(\\.exe)?$" -and $_.CommandLine -match "HytaleServer.jar") -or
            ($_.Name -eq "cmd.exe" -and $_.CommandLine -like "*HyTaleDevServer*" -and $_.CommandLine -like "*start.bat*")
        }
    if (-not $listener -and -not $remainingProcesses) {
        break
    }
    Start-Sleep -Milliseconds 250
} while ((Get-Date) -lt $deadline)

Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$startScript`"" -WorkingDirectory $ServerRoot | Out-Null

$startupDeadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
do {
    $listener = Get-NetUDPEndpoint -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $Port }
    if ($listener) {
        Write-Host "Local Hytale dev server is listening on UDP $Port."
        exit 0
    }
    Start-Sleep -Milliseconds 500
} while ((Get-Date) -lt $startupDeadline)

throw "Timed out waiting for the local Hytale dev server to listen on UDP $Port."
