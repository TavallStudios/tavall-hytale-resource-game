function Write-SharedLogLine {
    param(
        [string]$Path,
        [string]$Message
    )

    $encoding = [System.Text.UTF8Encoding]::new($false)
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        try {
            $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
            try {
                $payload = $encoding.GetBytes($Message + [Environment]::NewLine)
                $stream.Write($payload, 0, $payload.Length)
                $stream.Flush()
            } finally {
                $stream.Dispose()
            }
            return
        } catch {
            Start-Sleep -Milliseconds 75
        }
    }

    throw "Failed to append to log file: $Path"
}

function Invoke-RemoteLoggedBash {
    param(
        [string]$SshAlias,
        [string]$Script,
        [string]$LogPath,
        [switch]$AllowFailure
    )

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
        $process.StandardInput.Write([System.IO.File]::ReadAllText($tempScript))
        $process.StandardInput.Close()
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()

        foreach ($line in ($stdout -split "`r?`n")) {
            if ($line -ne "") {
                Write-SharedLogLine -Path $LogPath -Message $line
                Write-Host $line
            }
        }
        foreach ($line in ($stderr -split "`r?`n")) {
            if ($line -ne "") {
                Write-SharedLogLine -Path $LogPath -Message $line
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

function Ensure-RemoteQuicBridge {
    param(
        [string]$SshAlias,
        [string]$BridgeSourcePath,
        [string]$LogPath,
        [string]$RemoteBridgeDir = "/srv/hytale-startup-patch-test/_bot/quic-bridge",
        [string]$BridgeHost = "127.0.0.1",
        [int]$BridgePort = 5520,
        [string]$ServerHost = "127.0.0.1",
        [int]$ServerPort = 5520,
        [string]$ServerRoot = "/srv/hytale-startup-patch-test"
    )

    $serverJarPath = "{0}/Server/HytaleServer.jar" -f $ServerRoot
    $remoteSourcePath = "$RemoteBridgeDir/HytaleQuicTcpBridge.java"
    Invoke-RemoteLoggedBash -SshAlias $SshAlias -Script ("mkdir -p {0}" -f $RemoteBridgeDir) -LogPath $LogPath | Out-Null
    $copyExitCode = & scp.exe -F C:\Users\TJ\.ssh\config $BridgeSourcePath "${SshAlias}:$remoteSourcePath" 2>&1 | Tee-Object -Variable scpOutput
    foreach ($line in $scpOutput) {
        if ($line -ne "") {
            Write-SharedLogLine -Path $LogPath -Message $line
            Write-Host $line
        }
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy QUIC bridge source."
    }

    $script = @'
set -e
mkdir -p {0}
rm -f {0}/*.class
javac -cp {6} -d {0} {1}
bridge_pid=$(lsof -ti tcp:{3} || true)
if [ -n "$bridge_pid" ]; then
  kill $bridge_pid || true
  sleep 1
fi
nohup java -cp {6}:{0} HytaleQuicTcpBridge {2} {3} {4} {5} > {0}/bridge.out 2>&1 < /dev/null &
for i in $(seq 1 30); do
  if lsof -ti tcp:{3} >/dev/null 2>&1; then
    echo BRIDGE_READY
    exit 0
  fi
  sleep 1
done
echo BRIDGE_START_FAILED
cat {0}/bridge.out || true
exit 1
'@ -f $RemoteBridgeDir, $remoteSourcePath, $BridgeHost, $BridgePort, $ServerHost, $ServerPort, $serverJarPath

    $result = Invoke-RemoteLoggedBash -SshAlias $SshAlias -Script $script -LogPath $LogPath
    if ($result -notmatch "BRIDGE_READY") {
        throw "Remote QUIC bridge did not report readiness."
    }
}

function Minimize-TranscriptArtifact {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path $Path)) {
        return
    }

    $fileInfo = Get-Item -LiteralPath $Path
    $summary = [ordered]@{
        retained = $false
        originalSizeBytes = $fileInfo.Length
        minimizedAt = (Get-Date).ToString("o")
        note = "Raw transcript removed after run to keep bot-logs compact. Re-run with a dedicated debug transcript path if full trace data is needed."
    }

    Remove-Item -LiteralPath $Path -Force
    $summary | ConvertTo-Json -Depth 4 | Set-Content -Path $Path -Encoding utf8
}
