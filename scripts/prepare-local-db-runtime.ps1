param(
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [switch]$RequireLiveDatabases
)

$ErrorActionPreference = "Stop"

$envScript = Join-Path $ServerRoot "local-db-env.cmd"
if (-not (Test-Path $envScript)) {
    throw "local-db-env.cmd not found at $envScript"
}
$documentsSshRoot = Join-Path ([Environment]::GetFolderPath("MyDocuments")) ".ssh"

function Get-CmdEnvironmentValue {
    param(
        [string]$CommandFile,
        [string]$VariableName
    )

    $output = cmd /v:on /c "call `"$CommandFile`" >nul && echo !$VariableName!"
    return ($output | Select-Object -Last 1).Trim()
}

function Test-TcpPort {
    param(
        [string]$TargetHost,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($TargetHost, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne(1500)) {
            return $false
        }
        $client.EndConnect($asyncResult) | Out-Null
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-TcpPort {
    param(
        [string]$TargetHost,
        [int]$Port,
        [int]$TimeoutSeconds = 20
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Test-TcpPort -TargetHost $TargetHost -Port $Port) {
            return $true
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    return $false
}

function Ensure-SshTunnelFromLauncher {
    param(
        [string]$LauncherPath,
        [string]$TargetHost,
        [int]$Port,
        [string]$ProcessMatch
    )

    if (Test-TcpPort -TargetHost $TargetHost -Port $Port) {
        return $true
    }
    if (-not (Test-Path $LauncherPath)) {
        return $false
    }

    $existing = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^ssh(\.exe)?$|^powershell(\.exe)?$|^cmd\.exe$" -and $_.CommandLine -like "*$ProcessMatch*" }
    if (-not $existing) {
        $launcherCommand = (Get-Content -Raw -Path $LauncherPath).Trim()
        if ([string]::IsNullOrWhiteSpace($launcherCommand)) {
            return $false
        }
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $launcherCommand -WindowStyle Hidden | Out-Null
    }

    return Wait-TcpPort -TargetHost $TargetHost -Port $Port -TimeoutSeconds 20
}

$postgresUrl = Get-CmdEnvironmentValue -CommandFile $envScript -VariableName "TAVALL_POSTGRES_URL"
$redisHost = Get-CmdEnvironmentValue -CommandFile $envScript -VariableName "TAVALL_REDIS_HOST"
$redisPortRaw = Get-CmdEnvironmentValue -CommandFile $envScript -VariableName "TAVALL_REDIS_PORT"
$parsedRedisPort = 0
$redisPort = if ([int]::TryParse($redisPortRaw, [ref]$parsedRedisPort)) { $parsedRedisPort } else { 6379 }

$postgresHost = ""
$postgresPort = 5432
if ($postgresUrl -match "^jdbc:postgresql://([^:/]+)(?::(\d+))?/") {
    $postgresHost = $matches[1]
    if ($matches[2]) {
        $postgresPort = [int]$matches[2]
    }
}

$postgresReachable = $false
if (-not [string]::IsNullOrWhiteSpace($postgresHost)) {
    $postgresReachable = Test-TcpPort -TargetHost $postgresHost -Port $postgresPort
    if (-not $postgresReachable -and $postgresHost -eq "localhost") {
        $postgresReachable = Ensure-SshTunnelFromLauncher `
            -LauncherPath (Join-Path $documentsSshRoot "postgres_novus_tunnel.ps1") `
            -TargetHost $postgresHost `
            -Port $postgresPort `
            -ProcessMatch ("-L {0}:127.0.0.1:5432" -f $postgresPort)
    }
}

$redisReachable = $false
if (-not [string]::IsNullOrWhiteSpace($redisHost)) {
    $redisReachable = Test-TcpPort -TargetHost $redisHost -Port $redisPort
    if (-not $redisReachable -and $redisHost -eq "localhost") {
        $redisReachable = Ensure-SshTunnelFromLauncher `
            -LauncherPath (Join-Path $documentsSshRoot "redis_novus_tunnel.ps1") `
            -TargetHost $redisHost `
            -Port $redisPort `
            -ProcessMatch ("-L {0}:127.0.0.1:6379" -f $redisPort)
    }
}

$summary = [ordered]@{
    postgresUrl = $postgresUrl
    postgresHost = $postgresHost
    postgresPort = $postgresPort
    postgresReachable = $postgresReachable
    redisHost = $redisHost
    redisPort = $redisPort
    redisReachable = $redisReachable
}

$summary | ConvertTo-Json -Depth 4

if ($RequireLiveDatabases -and (-not $postgresReachable -or -not $redisReachable)) {
    throw "Local Redis/Postgres runtime is not fully reachable."
}
