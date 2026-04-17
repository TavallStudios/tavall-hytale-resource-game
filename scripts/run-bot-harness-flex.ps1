param(
    [ValidateSet("remote", "local")]
    [string]$BotLocation = "remote",
    [string]$Scenario = "connect-only",
    [string]$Username = "ResourceGameBot",
    [string]$LocalHarnessDir = "",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5522,
    [int]$BridgePort = 5522,
    [int]$RemoteForwardPort = 5523,
    [string]$AuthDomain = "",
    [string]$IdentityToken = "",
    [string]$SessionToken = "",
    [string]$AuthPassword = "",
    [string]$AuthScopes = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AuthDomain) -and -not [string]::IsNullOrWhiteSpace($env:HYTALE_AUTH_DOMAIN)) {
    $AuthDomain = $env:HYTALE_AUTH_DOMAIN
}

if ($BotLocation -eq "remote") {
    & (Join-Path $PSScriptRoot "run-remote-bot-harness-via-local.ps1") `
        -Scenario $Scenario `
        -Username $Username `
        -ServerPort $Port `
        -BridgePort $BridgePort `
        -RemoteForwardPort $RemoteForwardPort `
        -AuthDomain $AuthDomain `
        -IdentityToken $IdentityToken `
        -SessionToken $SessionToken `
        -AuthPassword $AuthPassword `
        -AuthScopes $AuthScopes `
        -StartBridge
    exit $LASTEXITCODE
}

if ([string]::IsNullOrWhiteSpace($LocalHarnessDir)) {
    throw "LocalHarnessDir is required when BotLocation=local"
}

$cliPath = Join-Path $LocalHarnessDir "apps/cli/dist/index.js"
if (-not (Test-Path $cliPath)) {
    throw "Local harness CLI not found at $cliPath"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $repoRoot "bot-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "local-{0}-{1}" -f $Scenario, $timestamp
$logPath = Join-Path $logDir ($baseName + ".log")
$summaryPath = Join-Path $logDir ($baseName + ".json")
$outputDir = Join-Path $logDir ($baseName + "-output")
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function Write-LogLine {
    param([string]$Message)
    Add-Content -Path $logPath -Value $Message -Encoding utf8
    Write-Host $Message
}

$startedAt = (Get-Date).ToString("o")
Write-LogLine ("[{0}] Starting local bot harness run" -f $startedAt)
Write-LogLine ("[{0}] Scenario={1}" -f (Get-Date).ToString("o"), $Scenario)
Write-LogLine ("[{0}] Host={1} Port={2}" -f (Get-Date).ToString("o"), $ServerHost, $Port)

Push-Location $LocalHarnessDir
try {
    $arguments = @($cliPath, "scenario", $Scenario, "--host", $ServerHost, "--port", $Port, "--username", $Username, "--output-dir", $outputDir, "--json")
    if (-not [string]::IsNullOrWhiteSpace($AuthDomain)) {
        $arguments += @("--auth-domain", $AuthDomain)
    }
    if (-not [string]::IsNullOrWhiteSpace($IdentityToken)) {
        $arguments += @("--identity-token", $IdentityToken)
    }
    if (-not [string]::IsNullOrWhiteSpace($SessionToken)) {
        $arguments += @("--session-token", $SessionToken)
    }
    if (-not [string]::IsNullOrWhiteSpace($AuthPassword)) {
        $arguments += @("--auth-password", $AuthPassword)
    }
    if (-not [string]::IsNullOrWhiteSpace($AuthScopes)) {
        $arguments += @("--auth-scopes", $AuthScopes)
    }

    $exitCode = (Start-Process -FilePath "node.exe" -ArgumentList $arguments -Wait -NoNewWindow -PassThru).ExitCode
} finally {
    Pop-Location
}

$summary = [ordered]@{
    startedAt = $startedAt
    completedAt = (Get-Date).ToString("o")
    botLocation = $BotLocation
    scenario = $Scenario
    host = $ServerHost
    port = $Port
    username = $Username
    harnessDir = $LocalHarnessDir
    outputDir = $outputDir
    exitCode = $exitCode
    success = ($exitCode -eq 0)
    logPath = $logPath
}

$summary | ConvertTo-Json | Set-Content -Path $summaryPath -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode
