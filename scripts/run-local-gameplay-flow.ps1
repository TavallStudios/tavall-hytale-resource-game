param(
    [Parameter(Mandatory = $true)]
    [string]$ScenarioScript,
    [string]$ScenarioName = "",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 5520,
    [string]$Username = "GameplayBot",
    [string]$StableUuid = "",
    [string]$LogDir = "",
    [string]$AuthDomain = "",
    [string]$IdentityToken = "",
    [string]$SessionToken = "",
    [string]$AuthPassword = "",
    [string]$AuthScopes = "",
    [string]$ServerJarPath = "C:\Users\TJ\Documents\HyTaleDevServer\HytaleServer.jar",
    [switch]$KeepTranscript
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "remote-quic-harness.ps1")

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $repoRoot "bot-logs"
}
if ([string]::IsNullOrWhiteSpace($ScenarioName)) {
    $ScenarioName = [System.IO.Path]::GetFileNameWithoutExtension($ScenarioScript)
}
if ([string]::IsNullOrWhiteSpace($StableUuid)) {
    $StableUuid = [guid]::NewGuid().ToString()
}
if ([string]::IsNullOrWhiteSpace($AuthDomain) -and -not [string]::IsNullOrWhiteSpace($env:HYTALE_AUTH_DOMAIN)) {
    $AuthDomain = $env:HYTALE_AUTH_DOMAIN
}

$scenarioPath = if ([System.IO.Path]::IsPathRooted($ScenarioScript)) {
    $ScenarioScript
} else {
    Join-Path $repoRoot $ScenarioScript
}
if (-not (Test-Path $scenarioPath)) {
    throw "Scenario script not found at $scenarioPath"
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseName = "local-{0}-{1}" -f $ScenarioName, $timestamp
$logPath = Join-Path $LogDir ($baseName + ".txt")
$summaryPath = Join-Path $LogDir ($baseName + ".txt.summary")
$outputDir = Join-Path $LogDir ($baseName + "-output")
$resultPath = Join-Path $outputDir "scenario-result.txt"
$tracePath = Join-Path $outputDir "transcript.txt"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function Write-LogLine {
    param([string]$Message)
    Add-Content -Path $logPath -Value $Message -Encoding utf8
    Write-Host $Message
}

function Invoke-ProcessCapture {
    param(
        [string]$FilePath,
        [string[]]$Arguments
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

        return [int]$process.ExitCode
    } finally {
        foreach ($path in @($stdoutPath, $stderrPath)) {
            if (Test-Path $path) {
                Remove-Item -Path $path -Force
            }
        }
    }
}

$previousAuthDomain = $env:HYTALE_AUTH_DOMAIN
$previousIdentityToken = $env:HYTALE_IDENTITY_TOKEN
$previousSessionToken = $env:HYTALE_SESSION_TOKEN
$previousAuthPassword = $env:HYTALE_AUTH_PASSWORD
$previousAuthScopes = $env:HYTALE_AUTH_SCOPES
$previousBotUuid = $env:HYTALE_BOT_UUID
$previousServerJar = $env:HYTALE_SERVER_JAR
$previousTraceMode = $env:RESOURCE_GAME_BOT_TRACE

$startedAt = (Get-Date).ToString("o")
$exitCode = 1
try {
    if (-not [string]::IsNullOrWhiteSpace($AuthDomain)) {
        $env:HYTALE_AUTH_DOMAIN = $AuthDomain
    }
    if (-not [string]::IsNullOrWhiteSpace($IdentityToken)) {
        $env:HYTALE_IDENTITY_TOKEN = $IdentityToken
    }
    if (-not [string]::IsNullOrWhiteSpace($SessionToken)) {
        $env:HYTALE_SESSION_TOKEN = $SessionToken
    }
    if (-not [string]::IsNullOrWhiteSpace($AuthPassword)) {
        $env:HYTALE_AUTH_PASSWORD = $AuthPassword
    }
    if (-not [string]::IsNullOrWhiteSpace($AuthScopes)) {
        $env:HYTALE_AUTH_SCOPES = $AuthScopes
    }
    if (-not [string]::IsNullOrWhiteSpace($StableUuid)) {
        $env:HYTALE_BOT_UUID = $StableUuid
    }
    if ([string]::IsNullOrWhiteSpace($env:HYTALE_SERVER_JAR) -and -not [string]::IsNullOrWhiteSpace($ServerJarPath)) {
        if (-not (Test-Path $ServerJarPath)) {
            throw "HYTALE_SERVER_JAR is not set and ServerJarPath does not exist: $ServerJarPath"
        }
        $env:HYTALE_SERVER_JAR = $ServerJarPath
    }
    if ([string]::IsNullOrWhiteSpace($env:RESOURCE_GAME_BOT_TRACE)) {
        $env:RESOURCE_GAME_BOT_TRACE = "compact"
    }

    $arguments = @($scenarioPath, $ServerHost, ([string]$Port), $Username, $StableUuid, $outputDir)

    Write-LogLine ("[{0}] Starting local gameplay flow" -f $startedAt)
    Write-LogLine ("[{0}] Scenario={1}" -f (Get-Date).ToString("o"), $ScenarioName)
    Write-LogLine ("[{0}] Script={1}" -f (Get-Date).ToString("o"), $scenarioPath)
    Write-LogLine ("[{0}] Host={1} Port={2}" -f (Get-Date).ToString("o"), $ServerHost, $Port)
    Write-LogLine ("[{0}] Username={1}" -f (Get-Date).ToString("o"), $Username)

    $exitCode = Invoke-ProcessCapture -FilePath "node.exe" -Arguments $arguments
} finally {
    if ($KeepTranscript -eq $false) {
        Minimize-TranscriptArtifact -Path $tracePath
    }
    $env:HYTALE_AUTH_DOMAIN = $previousAuthDomain
    $env:HYTALE_IDENTITY_TOKEN = $previousIdentityToken
    $env:HYTALE_SESSION_TOKEN = $previousSessionToken
    $env:HYTALE_AUTH_PASSWORD = $previousAuthPassword
    $env:HYTALE_AUTH_SCOPES = $previousAuthScopes
    $env:HYTALE_BOT_UUID = $previousBotUuid
    $env:HYTALE_SERVER_JAR = $previousServerJar
    $env:RESOURCE_GAME_BOT_TRACE = $previousTraceMode
}

$summaryLines = @(
    "startedAt=$startedAt",
    "completedAt=$((Get-Date).ToString('o'))",
    "scenario=$ScenarioName",
    "scriptPath=$scenarioPath",
    "host=$ServerHost",
    "port=$Port",
    "username=$Username",
    "stableUuid=$(if ([string]::IsNullOrWhiteSpace($StableUuid)) { '' } else { $StableUuid })",
    "exitCode=$exitCode",
    "success=$($exitCode -eq 0)",
    "logPath=$logPath",
    "resultPath=$(if (Test-Path $resultPath) { $resultPath } else { '' })",
    "transcriptPath=$(if (Test-Path $tracePath) { $tracePath } else { '' })"
)

Set-Content -Path $summaryPath -Value $summaryLines -Encoding utf8
Write-LogLine ("[{0}] SummaryFile={1}" -f (Get-Date).ToString("o"), $summaryPath)
Write-LogLine ("[{0}] ExitCode={1}" -f (Get-Date).ToString("o"), $exitCode)

exit $exitCode
