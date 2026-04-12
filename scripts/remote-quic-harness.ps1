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
                Add-Content -Path $LogPath -Value $line -Encoding utf8
                Write-Host $line
            }
        }
        foreach ($line in ($stderr -split "`r?`n")) {
            if ($line -ne "") {
                Add-Content -Path $LogPath -Value $line -Encoding utf8
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
        [string]$RemoteBridgeDir = "/srv/hytale/_bot/quic-bridge",
        [string]$BridgeHost = "127.0.0.1",
        [int]$BridgePort = 5520,
        [string]$ServerHost = "127.0.0.1",
        [int]$ServerPort = 5520
    )

    $remoteSourcePath = "$RemoteBridgeDir/HytaleQuicTcpBridge.java"
    Invoke-RemoteLoggedBash -SshAlias $SshAlias -Script ("mkdir -p {0}" -f $RemoteBridgeDir) -LogPath $LogPath | Out-Null
    $copyExitCode = & scp.exe -F C:\Users\TJ\.ssh\config $BridgeSourcePath "${SshAlias}:$remoteSourcePath" 2>&1 | Tee-Object -Variable scpOutput
    foreach ($line in $scpOutput) {
        if ($line -ne "") {
            Add-Content -Path $LogPath -Value $line -Encoding utf8
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
javac -cp /srv/hytale/Server/HytaleServer.jar -d {0} {1}
bridge_pid=$(lsof -ti tcp:{3} || true)
if [ -n "$bridge_pid" ]; then
  kill $bridge_pid || true
  sleep 1
fi
nohup java -cp /srv/hytale/Server/HytaleServer.jar:{0} HytaleQuicTcpBridge {2} {3} {4} {5} > {0}/bridge.out 2>&1 < /dev/null &
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
'@ -f $RemoteBridgeDir, $remoteSourcePath, $BridgeHost, $BridgePort, $ServerHost, $ServerPort

    $result = Invoke-RemoteLoggedBash -SshAlias $SshAlias -Script $script -LogPath $LogPath
    if ($result -notmatch "BRIDGE_READY") {
        throw "Remote QUIC bridge did not report readiness."
    }
}
