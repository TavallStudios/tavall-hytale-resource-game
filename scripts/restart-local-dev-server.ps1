param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [switch]$BuildPlugin,
    [switch]$DeployPlugin = $true,
    [switch]$RequireLiveDatabases,
    [switch]$ResetProblemRegions,
    [int]$Port = 5520,
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$startScript = Join-Path $ServerRoot "start.bat"
if (-not (Test-Path $startScript)) {
    throw "start.bat not found at $startScript"
}

$prepareDbScript = Join-Path $PSScriptRoot "prepare-local-db-runtime.ps1"
if (Test-Path $prepareDbScript) {
    try {
        & $prepareDbScript -ServerRoot $ServerRoot -RequireLiveDatabases:$RequireLiveDatabases | Out-Host
    } catch {
        if ($RequireLiveDatabases) {
            throw
        }
        Write-Warning ("Local database runtime is not fully reachable. The plugin will fall back to in-memory persistence if needed. {0}" -f $_.Exception.Message)
    }
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

$listenerProcesses = Get-NetUDPEndpoint -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -eq $Port } |
    Select-Object -ExpandProperty OwningProcess -Unique

$allProcessIds = @(
    $serverProcesses | Select-Object -ExpandProperty ProcessId
    $launcherProcesses | Select-Object -ExpandProperty ProcessId
    $listenerProcesses
) | Sort-Object -Unique

foreach ($processId in $allProcessIds) {
    try {
        $taskKillOutput = & taskkill.exe /F /T /PID $processId 2>&1
        $taskKillExitCode = $LASTEXITCODE
        if ($taskKillExitCode -ne 0 -and (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
            Write-Warning ("Failed to stop local Hytale process tree {0}: {1}" -f $processId, ($taskKillOutput -join " "))
        }
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

if ($ResetProblemRegions) {
    $chunksDir = Join-Path $ServerRoot "universe\\worlds\\default\\chunks"
    $regionFiles = @(
        Join-Path $chunksDir "-2.0.region.bin"
    )
    $suffix = Get-Date -Format "yyyyMMdd-HHmmss"
    foreach ($regionPath in $regionFiles) {
        if (-not (Test-Path $regionPath)) {
            continue
        }
        $backupPath = "$regionPath.corrupt.$suffix"
        Move-Item -LiteralPath $regionPath -Destination $backupPath -Force
        Write-Host ("Renamed corrupt region file: {0} -> {1}" -f $regionPath, $backupPath)
    }
}

$launchProcess = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$startScript`"" -WorkingDirectory $ServerRoot -PassThru

$startupDeadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
do {
    $listener = Get-NetUDPEndpoint -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $Port }
    $activeListeners = @($listener | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($activeListeners.Count -ge 1) {
        Write-Host "Local Hytale dev server is listening on UDP $Port (processes: $($activeListeners -join ', '))."
        exit 0
    }
    if ($launchProcess.HasExited) {
        throw "Local Hytale launcher exited before UDP $Port came online."
    }
    Start-Sleep -Milliseconds 500
} while ((Get-Date) -lt $startupDeadline)

throw "Timed out waiting for the local Hytale dev server to listen on UDP $Port."
