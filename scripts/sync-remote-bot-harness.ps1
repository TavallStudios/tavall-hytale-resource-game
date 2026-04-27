param(
    [string]$SshAlias = "novus-remote",
    [string]$SshConfigPath = "C:\Users\TJ\.ssh\config",
    [string]$RemoteHarnessDir = "/srv/hytale/_bot/hytale-sim",
    [string]$BotRepoRoot = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($BotRepoRoot)) {
    $monoRoot = Split-Path -Parent (Split-Path -Parent $repoRoot)
    $BotRepoRoot = Join-Path $monoRoot "tavall-java-game-tools\hytale-bots"
}

if (-not (Test-Path -LiteralPath $BotRepoRoot)) {
    throw "Bot harness repo not found: $BotRepoRoot"
}

if (-not $SkipBuild) {
    Push-Location $BotRepoRoot
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) {
            throw "Bot harness build failed."
        }
    } finally {
        Pop-Location
    }
}

function Copy-RemoteDirectoryContents {
    param(
        [string]$LocalDirectory,
        [string]$RemoteDirectory
    )

    if (-not (Test-Path -LiteralPath $LocalDirectory)) {
        throw "Local directory not found: $LocalDirectory"
    }

    $mkdirCommand = "mkdir -p '$RemoteDirectory'"
    ssh.exe -F $SshConfigPath $SshAlias $mkdirCommand | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create remote directory: $RemoteDirectory"
    }

    scp.exe -F $SshConfigPath -r (Join-Path $LocalDirectory "*") ("{0}:{1}/" -f $SshAlias, $RemoteDirectory) | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy $LocalDirectory to $RemoteDirectory"
    }
}

Copy-RemoteDirectoryContents -LocalDirectory (Join-Path $BotRepoRoot "packages\client\dist") -RemoteDirectory "$RemoteHarnessDir/packages/client/dist"
Copy-RemoteDirectoryContents -LocalDirectory (Join-Path $BotRepoRoot "packages\protocol\dist") -RemoteDirectory "$RemoteHarnessDir/packages/protocol/dist"
Copy-RemoteDirectoryContents -LocalDirectory (Join-Path $BotRepoRoot "packages\protocol\generated") -RemoteDirectory "$RemoteHarnessDir/packages/protocol/generated"
Copy-RemoteDirectoryContents -LocalDirectory (Join-Path $BotRepoRoot "packages\scenario\dist") -RemoteDirectory "$RemoteHarnessDir/packages/scenario/dist"
Copy-RemoteDirectoryContents -LocalDirectory (Join-Path $BotRepoRoot "apps\cli\dist") -RemoteDirectory "$RemoteHarnessDir/apps/cli/dist"

Write-Host "Remote bot harness synchronized: $RemoteHarnessDir"
