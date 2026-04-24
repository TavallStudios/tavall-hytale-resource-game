param(
    [string]$NovusEnvScript = "C:\\Users\\TJ\\Documents\\.ssh\\postgre_env_variables.ps1",
    [string]$PostgresUrl = "",
    [string]$PostgresUser = "",
    [string]$PostgresPassword = "",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 16379,
    [string]$RedisPassword = "",
    [bool]$RedisTls = $false,
    [string]$KingdomTimezone = "America/Los_Angeles"
)

$ErrorActionPreference = "Stop"

if (Test-Path $NovusEnvScript) {
    . $NovusEnvScript
}

if ([string]::IsNullOrWhiteSpace($PostgresUrl)) {
    if (-not [string]::IsNullOrWhiteSpace($env:NOVUS_POSTGRES_URL)) {
        $PostgresUrl = $env:NOVUS_POSTGRES_URL
    } else {
        $PostgresUrl = "jdbc:postgresql://localhost:15432/tavall"
    }
}
if ([string]::IsNullOrWhiteSpace($PostgresUser)) {
    if (-not [string]::IsNullOrWhiteSpace($env:NOVUS_POSTGRES_USER)) {
        $PostgresUser = $env:NOVUS_POSTGRES_USER
    }
}
if ([string]::IsNullOrWhiteSpace($PostgresPassword)) {
    if (-not [string]::IsNullOrWhiteSpace($env:NOVUS_POSTGRES_PASS)) {
        $PostgresPassword = $env:NOVUS_POSTGRES_PASS
    }
}

$env:TAVALL_POSTGRES_URL = $PostgresUrl
if (-not [string]::IsNullOrWhiteSpace($PostgresUser)) {
    $env:TAVALL_POSTGRES_USER = $PostgresUser
}
if (-not [string]::IsNullOrWhiteSpace($PostgresPassword)) {
    $env:TAVALL_POSTGRES_PASSWORD = $PostgresPassword
}
$env:TAVALL_REDIS_HOST = $RedisHost
$env:TAVALL_REDIS_PORT = $RedisPort.ToString()
$env:TAVALL_REDIS_PASSWORD = $RedisPassword
$env:TAVALL_REDIS_TLS = $RedisTls.ToString().ToLowerInvariant()
$env:TAVALL_KINGDOM_TIMEZONE = $KingdomTimezone

Write-Host "Set TAVALL_* environment variables for local singleplayer."