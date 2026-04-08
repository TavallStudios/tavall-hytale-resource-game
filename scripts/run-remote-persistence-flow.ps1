param(
    [string]$SshAlias = "novus-remote",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$ScenarioScriptPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/scripts/remote-persistence-flow.mjs",
    [string]$PluginJarPath = "F:/workspace/TavallMonoRepo/tavall-java-hytale-games/tavall-hytale-resource-game/target/tavall-hytale-resource-game.jar",
    [string]$RemotePluginJarPath = "/srv/hytale/Server/mods/tavall-hytale-resource-game.jar",
    [string]$ServerRoot = "/srv/hytale",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5520,
    [string]$Username = "PersistenceBot",
    [string]$StableUuid = "123e4567-e89b-12d3-a456-426614174000",
    [string]$PostgresContainerName = "tavall-resource-game-postgres",
    [int]$PostgresPort = 55432,
    [string]$PostgresDatabase = "tavall_resource_game",
    [string]$PostgresUser = "tavall_resource_game",
    [string]$PostgresPassword = "tavall_resource_game_dev",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$LogDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "remote-persistence-flow-{0}" -f $timestamp
$logPath = Join-Path $LogDir ($baseName + ".log")
$summaryPath = Join-Path $LogDir ($baseName + ".json")
$phaseOneResultPath = Join-Path $LogDir ($baseName + "-seed-result.json")
$phaseOneTracePath = Join-Path $LogDir ($baseName + "-seed-transcript.json")
$phaseTwoResultPath = Join-Path $LogDir ($baseName + "-verify-result.json")
$phaseTwoTracePath = Join-Path $LogDir ($baseName + "-verify-transcript.json")
$schemaTempPath = Join-Path $env:TEMP ($baseName + "-schema.sql")
$remoteScriptPath = "/tmp/{0}.mjs" -f $baseName
$remoteSchemaPath = "/tmp/{0}-schema.sql" -f $baseName
$remotePhaseOneDir = "/tmp/{0}-seed" -f $baseName
$remotePhaseTwoDir = "/tmp/{0}-verify" -f $baseName
$postgresVolumeName = "{0}-data" -f $PostgresContainerName
$serverLogPathFile = Join-Path $LogDir ($baseName + "-server-log.txt")
$dbSnapshotPath = Join-Path $LogDir ($baseName + "-db-snapshot.txt")

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
    param(
        [string]$Script,
        [switch]$AllowFailure
    )

    $tempScript = Join-Path $env:TEMP ([System.IO.Path]::GetRandomFileName() + ".sh")
    try {
        $normalizedScript = $Script -replace "`r`n", "`n"
        [System.IO.File]::WriteAllText($tempScript, $normalizedScript, (New-Object System.Text.UTF8Encoding($false)))
        $arguments = @(
            "-F", "C:\Users\TJ\.ssh\config",
            $SshAlias,
            "bash -s"
        )
        $stdin = Get-Content -Raw -Path $tempScript
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
        $process.StandardInput.Write($stdin)
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
        if (-not $AllowFailure -and $process.ExitCode -ne 0) {
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
    $jdbcUrl = "jdbc:postgresql://127.0.0.1:{0}/{1}" -f $PostgresPort, $PostgresDatabase
    $script = @'
set -e
pid=$(lsof -ti tcp:{0} || true)
if [ -n "$pid" ]; then
  kill $pid || true
  sleep 2
fi
cd {1}
export TAVALL_POSTGRES_URL='{2}'
export TAVALL_POSTGRES_USER='{3}'
export TAVALL_POSTGRES_PASSWORD='{4}'
export TAVALL_REDIS_HOST='{5}'
export TAVALL_REDIS_PORT='{6}'
export TAVALL_REDIS_PASSWORD=''
export TAVALL_REDIS_TLS='false'
nohup ./start.sh --transport TCP --auth-mode INSECURE --allow-op > start.out 2>&1 < /dev/null &
for i in $(seq 1 60); do
  if lsof -ti tcp:{0} >/dev/null 2>&1; then
    echo SERVER_READY
    exit 0
  fi
  sleep 2
done
exit 1
'@ -f $Port, $ServerRoot, $jdbcUrl, $PostgresUser, $PostgresPassword, $RedisHost, $RedisPort
    Invoke-RemoteBash -Script $script | Out-Null
}

function Invoke-RemoteScenario {
    param(
        [string]$Mode,
        [string]$RemoteOutputDir,
        [string]$LocalResultPath,
        [string]$LocalTracePath
    )

    $remoteCommand = "cd $RemoteHarnessDir && mkdir -p $RemoteOutputDir && node $remoteScriptPath $Mode $ServerHost $Port $Username $StableUuid $RemoteOutputDir"
    $exitCode = Invoke-ProcessCapture -FilePath "ssh.exe" -Arguments @(
        "-F", "C:\Users\TJ\.ssh\config",
        $SshAlias,
        $remoteCommand
    ) -AllowFailure
    if ($exitCode -ne 0) {
        throw "Remote $Mode scenario failed"
    }
    Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
        "-F", "C:\Users\TJ\.ssh\config",
        ("{0}:{1}/scenario-result.json" -f $SshAlias, $RemoteOutputDir),
        $LocalResultPath
    ) | Out-Null
    Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
        "-F", "C:\Users\TJ\.ssh\config",
        ("{0}:{1}/transcript.json" -f $SshAlias, $RemoteOutputDir),
        $LocalTracePath
    ) | Out-Null
}

$schema = @(
    (Get-Content -Raw -Path (Join-Path $repoRoot "schema/postgres/001_player_profile.sql")),
    (Get-Content -Raw -Path (Join-Path $repoRoot "schema/postgres/002_player_game_state.sql"))
) -join "`n`n"
Set-Content -Path $schemaTempPath -Value $schema -Encoding utf8

$startedAt = (Get-Date).ToString("o")
Write-LogLine ("[{0}] Starting remote persistence flow" -f $startedAt)
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
Invoke-ProcessCapture -FilePath "scp.exe" -Arguments @(
    "-F", "C:\Users\TJ\.ssh\config",
    $schemaTempPath,
    ("{0}:{1}" -f $SshAlias, $remoteSchemaPath)
) | Out-Null

$bootstrapScript = @'
set -e
docker rm -f {1} >/dev/null 2>&1 || true
docker volume rm -f {7} >/dev/null 2>&1 || true
docker run -d \
  --name {1} \
  -e POSTGRES_DB={2} \
  -e POSTGRES_USER={3} \
  -e POSTGRES_PASSWORD={4} \
  -p 127.0.0.1:{5}:5432 \
  -v {7}:/var/lib/postgresql/data \
  postgres:16-alpine >/dev/null
for i in $(seq 1 60); do
  if PGPASSWORD='{4}' psql -h 127.0.0.1 -p {5} -U {3} -d {2} -Atqc 'SELECT 1' >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
PGPASSWORD='{4}' psql -h 127.0.0.1 -p {5} -U {3} -d {2} -f {6}
PGPASSWORD='{4}' psql -h 127.0.0.1 -p {5} -U {3} -d {2} -c 'TRUNCATE TABLE player_game_state, player_profile RESTART IDENTITY CASCADE;'
redis-cli FLUSHDB >/dev/null
'@ -f $ServerRoot, $PostgresContainerName, $PostgresDatabase, $PostgresUser, $PostgresPassword, $PostgresPort, $remoteSchemaPath, $postgresVolumeName
Invoke-RemoteBash -Script $bootstrapScript | Out-Null

Restart-RemoteServer
Start-Sleep -Seconds 2
Invoke-RemoteScenario -Mode "seed" -RemoteOutputDir $remotePhaseOneDir -LocalResultPath $phaseOneResultPath -LocalTracePath $phaseOneTracePath

$dbQuery = "SELECT p.uuid, g.citizen_count, g.troop_count, g.food, g.wood, g.iron, g.interior_world FROM player_profile p JOIN player_game_state g ON g.profile_id = p.id ORDER BY p.id;"
$dbSnapshotScript = @'
set -e
PGPASSWORD='{0}' psql -h 127.0.0.1 -p {1} -U {2} -d {3} -A -F '|' -t -c "{4}"
redis-cli KEYS 'player-game-state:*'
redis-cli KEYS 'player-profile:*'
'@ -f $PostgresPassword, $PostgresPort, $PostgresUser, $PostgresDatabase, $dbQuery
$dbSnapshot = Invoke-RemoteBash -Script $dbSnapshotScript
Set-Content -Path $dbSnapshotPath -Value $dbSnapshot -Encoding utf8

Restart-RemoteServer
Start-Sleep -Seconds 2
Invoke-RemoteScenario -Mode "verify" -RemoteOutputDir $remotePhaseTwoDir -LocalResultPath $phaseTwoResultPath -LocalTracePath $phaseTwoTracePath

$serverLogPathScript = @'
set -e
ls -1t {0}/Server/logs/*_server.log | head -n 1
'@ -f $ServerRoot
$serverLogPath = Invoke-RemoteBash -Script $serverLogPathScript
Set-Content -Path $serverLogPathFile -Value $serverLogPath -Encoding utf8

$cacheEvidenceScript = @'
set -e
log_file=$(ls -1t {0}/Server/logs/*_server.log | head -n 1)
grep -n '{1}\|Player profile cache hit\|Player game state cache hit\|Population displays ready' "$log_file" | tail -n 80
'@ -f $ServerRoot, $StableUuid
$cacheEvidence = Invoke-RemoteBash -Script $cacheEvidenceScript
if ($cacheEvidence -notmatch "Player profile cache hit for $StableUuid" -or $cacheEvidence -notmatch "Player game state cache hit for $StableUuid") {
    throw "Redis-first cache hit evidence not found in server log."
}
if ($cacheEvidence -notmatch "Population displays ready for $StableUuid") {
    throw "Population display readiness evidence not found in server log."
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    sshAlias = $SshAlias
    host = $ServerHost
    port = $Port
    username = $Username
    stableUuid = $StableUuid
    postgresContainerName = $PostgresContainerName
    postgresPort = $PostgresPort
    postgresDatabase = $PostgresDatabase
    redisHost = $RedisHost
    redisPort = $RedisPort
    success = $true
    logPath = $logPath
    seedResultPath = $phaseOneResultPath
    seedTracePath = $phaseOneTracePath
    verifyResultPath = $phaseTwoResultPath
    verifyTracePath = $phaseTwoTracePath
    dbSnapshotPath = $dbSnapshotPath
    serverLogPath = $serverLogPathFile
}

$summary | ConvertTo-Json | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] Persistence flow passed" -f (Get-Date).ToString("o"))

if (Test-Path $schemaTempPath) {
    Remove-Item -Path $schemaTempPath -Force
}
