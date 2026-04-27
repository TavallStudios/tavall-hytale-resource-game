param(
    [string]$DesktopAutomationRoot = "F:\workspace\TavallMonoRepo\tavall-java-ai-apps\tavall-ai-agent-task-manager\clients\desktop",
    [string]$ServerRoot = "C:\Users\TJ\Documents\HyTaleDevServer",
    [string]$ArtifactDir = "",
    [string]$OpenMenuCommand = "/kd ui",
    [ValidateSet("server-file", "window-message")]
    [string]$ControlMode = "server-file",
    [string]$ControlPlayer = "*",
    [string]$ControlUi = "debug",
    [string]$ChatOpenKey = "Enter",
    [int]$WindowTimeoutMs = 30000,
    [int]$ControlAckTimeoutMs = 10000,
    [int]$SampleStep = 6,
    [double]$MinimumOverlayCoverage = 0.012,
    [double]$MinimumTemplateScore = 0.68,
    [double]$MinimumCenterChangedCoverage = 0.02,
    [switch]$SkipAutomationBuild,
    [switch]$SkipCommand,
    [switch]$SkipTemplateCheck,
    [switch]$SkipVisualDelta,
    [switch]$NoRestoreForeground
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ArtifactDir)) {
    $ArtifactDir = Join-Path $repoRoot ("bot-logs\hytale-visual-verification-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
}

$dotnet = "C:\Program Files\dotnet\dotnet.exe"
$hostProjectPath = Join-Path $DesktopAutomationRoot "AgentTaskManager.AutomationHost\AgentTaskManager.AutomationHost.csproj"
$hostDll = Join-Path $DesktopAutomationRoot "AgentTaskManager.AutomationHost\bin\x64\Debug\net8.0-windows10.0.19041.0\AgentTaskManager.AutomationHost.dll"
$clientWindowTarget = @{ processName = "HytaleClient" }

Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies "System.Drawing" -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using System.Text;

public static class ResourceGameVisualWindow {
    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
}

public sealed class ResourceGameVisualTemplateMatchResult {
    public bool Matched { get; set; }
    public double Score { get; set; }
    public int X { get; set; }
    public int Y { get; set; }
    public int Width { get; set; }
    public int Height { get; set; }
    public string OutputPath { get; set; }
}

public static class ResourceGameVisualTemplateMatcher {
    public static ResourceGameVisualTemplateMatchResult Match(
        string sourcePath,
        string templatePath,
        int left,
        int top,
        int width,
        int height,
        double threshold,
        string outputPath
    ) {
        using (Bitmap sourceOriginal = new Bitmap(sourcePath))
        using (Bitmap templateOriginal = new Bitmap(templatePath))
        using (Bitmap source = CloneArgb(sourceOriginal))
        using (Bitmap template = CloneArgb(templateOriginal)) {
            int searchLeft = Math.Max(0, left);
            int searchTop = Math.Max(0, top);
            int searchRight = Math.Min(source.Width, left + width);
            int searchBottom = Math.Min(source.Height, top + height);
            if (searchRight - searchLeft < template.Width || searchBottom - searchTop < template.Height) {
                throw new InvalidOperationException("The UI template search region is smaller than the template.");
            }

            Rectangle sourceBounds = new Rectangle(0, 0, source.Width, source.Height);
            Rectangle templateBounds = new Rectangle(0, 0, template.Width, template.Height);
            BitmapData sourceData = source.LockBits(sourceBounds, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            BitmapData templateData = template.LockBits(templateBounds, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            try {
                byte[] sourceBytes = new byte[Math.Abs(sourceData.Stride) * source.Height];
                byte[] templateBytes = new byte[Math.Abs(templateData.Stride) * template.Height];
                Marshal.Copy(sourceData.Scan0, sourceBytes, 0, sourceBytes.Length);
                Marshal.Copy(templateData.Scan0, templateBytes, 0, templateBytes.Length);

                List<int> maskX = new List<int>();
                List<int> maskY = new List<int>();
                List<byte> maskR = new List<byte>();
                List<byte> maskG = new List<byte>();
                List<byte> maskB = new List<byte>();
                for (int y = 0; y < template.Height; y++) {
                    int row = y * templateData.Stride;
                    for (int x = 0; x < template.Width; x++) {
                        int pixel = row + (x * 4);
                        byte alpha = templateBytes[pixel + 3];
                        if (alpha < 160) {
                            continue;
                        }
                        maskX.Add(x);
                        maskY.Add(y);
                        maskB.Add(templateBytes[pixel]);
                        maskG.Add(templateBytes[pixel + 1]);
                        maskR.Add(templateBytes[pixel + 2]);
                    }
                }

                if (maskX.Count == 0) {
                    throw new InvalidOperationException("The UI template does not contain enough opaque pixels to match.");
                }

                long bestDifference = long.MaxValue;
                int bestX = searchLeft;
                int bestY = searchTop;
                for (int candidateY = searchTop; candidateY <= searchBottom - template.Height; candidateY++) {
                    for (int candidateX = searchLeft; candidateX <= searchRight - template.Width; candidateX++) {
                        long difference = 0;
                        for (int index = 0; index < maskX.Count; index++) {
                            int sourcePixel = ((candidateY + maskY[index]) * sourceData.Stride) + ((candidateX + maskX[index]) * 4);
                            difference += Math.Abs(sourceBytes[sourcePixel] - maskB[index]);
                            difference += Math.Abs(sourceBytes[sourcePixel + 1] - maskG[index]);
                            difference += Math.Abs(sourceBytes[sourcePixel + 2] - maskR[index]);
                            if (difference >= bestDifference) {
                                break;
                            }
                        }

                        if (difference < bestDifference) {
                            bestDifference = difference;
                            bestX = candidateX;
                            bestY = candidateY;
                        }
                    }
                }

                double maxDifference = maskX.Count * 765.0d;
                double score = 1.0d - (bestDifference / maxDifference);
                WriteAnnotated(sourcePath, bestX, bestY, template.Width, template.Height, outputPath);
                return new ResourceGameVisualTemplateMatchResult {
                    Matched = score >= threshold,
                    Score = score,
                    X = bestX,
                    Y = bestY,
                    Width = template.Width,
                    Height = template.Height,
                    OutputPath = outputPath
                };
            }
            finally {
                source.UnlockBits(sourceData);
                template.UnlockBits(templateData);
            }
        }
    }

    private static Bitmap CloneArgb(Bitmap bitmap) {
        Rectangle bounds = new Rectangle(0, 0, bitmap.Width, bitmap.Height);
        return bitmap.PixelFormat == PixelFormat.Format32bppArgb
            ? new Bitmap(bitmap)
            : bitmap.Clone(bounds, PixelFormat.Format32bppArgb);
    }

    private static void WriteAnnotated(string sourcePath, int x, int y, int width, int height, string outputPath) {
        using (Bitmap source = new Bitmap(sourcePath))
        using (Graphics graphics = Graphics.FromImage(source))
        using (Pen pen = new Pen(Color.Red, 3)) {
            graphics.DrawRectangle(pen, x, y, Math.Max(1, width), Math.Max(1, height));
            string directory = System.IO.Path.GetDirectoryName(outputPath);
            if (!string.IsNullOrWhiteSpace(directory)) {
                System.IO.Directory.CreateDirectory(directory);
            }
            source.Save(outputPath, ImageFormat.Png);
        }
    }
}
'@

function Invoke-AutomationRequest {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Body
    )

    $json = $Body | ConvertTo-Json -Compress -Depth 12
    $response = $json | & $dotnet $hostDll
    if ($LASTEXITCODE -ne 0) {
        throw "Automation host failed while executing request: $json`nHost response: $response"
    }

    $parsed = ([string]$response).TrimStart([char]0xFEFF) | ConvertFrom-Json
    if (-not $parsed.ok) {
        throw "Automation request failed: $($parsed.error.message)"
    }

    return $parsed.result
}

function Wait-HytaleWindow {
    return Invoke-AutomationRequest @{
        id = "wait-hytale-window"
        command = "wait_for_window"
        parameters = @{
            window = $clientWindowTarget
            timeoutMs = $WindowTimeoutMs
            includeInvisible = $true
        }
    }
}

function Capture-HytaleWindow {
    param(
        [Parameter(Mandatory = $true)]
        [string]$OutputPath
    )

    try {
        return Invoke-AutomationRequest @{
            id = "capture-window"
            command = "capture_window"
            parameters = @{
                window = $clientWindowTarget
                outputPath = $OutputPath
                allowScreenCopyFallback = $true
            }
        }
    }
    catch {
        return Invoke-AutomationRequest @{
            id = "capture-stream-frame"
            command = "capture_stream_frame"
            parameters = @{
                window = $clientWindowTarget
                outputPath = $OutputPath
                allowScreenCopyFallback = $true
                includeBase64 = $false
            }
        }
    }
}

function Send-HytaleKey {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Key,
        [int]$DelayMs = 150
    )

    return Invoke-AutomationRequest @{
        id = "send-key-$Key"
        command = "send_key_batch"
        parameters = @{
            window = $clientWindowTarget
            activateWindow = $false
            events = @(
                @{
                    key = $Key
                    action = "press"
                    delayMs = $DelayMs
                }
            )
        }
    }
}

function Send-HytaleText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    return Invoke-AutomationRequest @{
        id = "send-text"
        command = "send_text"
        parameters = @{
            window = $clientWindowTarget
            text = $Text
            activateWindow = $false
        }
    }
}

function Open-KingdomMenu {
    if ($ControlMode -eq "server-file") {
        return Invoke-ServerControlOpenUi
    }

    Send-HytaleKey -Key "Escape" -DelayMs 100 | Out-Null
    Start-Sleep -Milliseconds 200
    Send-HytaleKey -Key $ChatOpenKey -DelayMs 150 | Out-Null
    Start-Sleep -Milliseconds 200
    Send-HytaleText -Text $OpenMenuCommand | Out-Null
    Start-Sleep -Milliseconds 200
    Send-HytaleText -Text ([string][char]13) | Out-Null
    Start-Sleep -Milliseconds 200
    Send-HytaleKey -Key "Enter" -DelayMs 250 | Out-Null
    Start-Sleep -Seconds 2
    return $null
}

function Invoke-ServerControlRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Ui,
        [Parameter(Mandatory = $true)]
        [string]$ExpectedStatus
    )

    $controlRoot = Join-Path $ServerRoot "visual-control"
    $requestPath = Join-Path $controlRoot "resource-game-ui-request.properties"
    $ackPath = Join-Path $controlRoot "resource-game-ui-request.ack.properties"
    $requestId = [guid]::NewGuid().ToString("N")
    New-Item -ItemType Directory -Force -Path $controlRoot | Out-Null
    if (Test-Path -LiteralPath $ackPath) {
        Remove-Item -LiteralPath $ackPath -Force
    }

    $requestLines = @(
        "requestId=$requestId",
        "player=$ControlPlayer",
        "ui=$Ui"
    )
    $tempPath = "$requestPath.tmp"
    [System.IO.File]::WriteAllLines(
        $tempPath,
        [string[]]$requestLines,
        [System.Text.UTF8Encoding]::new($false)
    )
    Move-Item -LiteralPath $tempPath -Destination $requestPath -Force

    $deadline = [DateTimeOffset]::UtcNow.AddMilliseconds($ControlAckTimeoutMs)
    do {
        if (Test-Path -LiteralPath $ackPath) {
            $ack = @{}
            foreach ($line in Get-Content -LiteralPath $ackPath) {
                if ($line.StartsWith("#") -or -not $line.Contains("=")) {
                    continue
                }
                $parts = $line.Split("=", 2)
                $ack[$parts[0]] = $parts[1]
            }
            if ($ack.requestId -eq $requestId) {
                if ($ack.status -ne $ExpectedStatus) {
                    throw "Server visual control failed: status=$($ack.status) message=$($ack.message)"
                }
                Start-Sleep -Milliseconds 500
                return [PSCustomObject]$ack
            }
        }
        Start-Sleep -Milliseconds 200
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    throw "Timed out waiting for server visual control ack: $ackPath"
}

function Invoke-ServerControlOpenUi {
    return Invoke-ServerControlRequest -Ui $ControlUi -ExpectedStatus "opened"
}

function Invoke-ServerControlCloseUi {
    return Invoke-ServerControlRequest -Ui "close" -ExpectedStatus "closed"
}

function Get-ForegroundWindowSnapshot {
    $handle = [ResourceGameVisualWindow]::GetForegroundWindow()
    $builder = New-Object System.Text.StringBuilder 512
    [void][ResourceGameVisualWindow]::GetWindowText($handle, $builder, $builder.Capacity)
    return [PSCustomObject]@{
        handle = $handle.ToInt64()
        title = $builder.ToString()
        display = ("{0}:{1}" -f $handle.ToInt64().ToString("X"), $builder.ToString())
    }
}

function Restore-ForegroundWindow {
    param(
        [Parameter(Mandatory = $true)]
        [long]$Handle
    )

    if ($Handle -le 0) {
        return $false
    }

    return [ResourceGameVisualWindow]::SetForegroundWindow([IntPtr]$Handle)
}

function Measure-Capture {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $brightnessSum = 0.0D
        $brightnessSquareSum = 0.0D
        $sampleCount = 0
        $darkPanelCount = 0
        $blueAccentCount = 0
        $goldAccentCount = 0
        $nonBlackCount = 0

        for ($sampleY = 0; $sampleY -lt $bitmap.Height; $sampleY += $SampleStep) {
            for ($sampleX = 0; $sampleX -lt $bitmap.Width; $sampleX += $SampleStep) {
                $pixel = $bitmap.GetPixel($sampleX, $sampleY)
                $brightness = ($pixel.R + $pixel.G + $pixel.B) / 3.0D
                $brightnessSum += $brightness
                $brightnessSquareSum += ($brightness * $brightness)
                $sampleCount++

                if ($brightness -gt 8) {
                    $nonBlackCount++
                }
                if ($pixel.R -le 70 -and $pixel.G -le 85 -and $pixel.B -le 110 -and $brightness -ge 18) {
                    $darkPanelCount++
                }
                if ($pixel.B -ge 95 -and $pixel.G -ge 60 -and $pixel.R -le 105) {
                    $blueAccentCount++
                }
                if ($pixel.R -ge 135 -and $pixel.G -ge 95 -and $pixel.B -le 90) {
                    $goldAccentCount++
                }
            }
        }

        $averageBrightness = $brightnessSum / [Math]::Max(1, $sampleCount)
        $variance = ($brightnessSquareSum / [Math]::Max(1, $sampleCount)) - ($averageBrightness * $averageBrightness)
        if ($variance -lt 0) {
            $variance = 0
        }

        return [PSCustomObject]@{
            width = $bitmap.Width
            height = $bitmap.Height
            sampleCount = $sampleCount
            averageBrightness = [Math]::Round($averageBrightness, 3)
            contrastEstimate = [Math]::Round([Math]::Sqrt($variance), 3)
            nonBlackCoverage = [Math]::Round($nonBlackCount / [Math]::Max(1, $sampleCount), 5)
            darkPanelCoverage = [Math]::Round($darkPanelCount / [Math]::Max(1, $sampleCount), 5)
            blueAccentCoverage = [Math]::Round($blueAccentCount / [Math]::Max(1, $sampleCount), 5)
            goldAccentCoverage = [Math]::Round($goldAccentCount / [Math]::Max(1, $sampleCount), 5)
        }
    }
    finally {
        $bitmap.Dispose()
    }
}

function Compare-Captures {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BeforePath,
        [Parameter(Mandatory = $true)]
        [string]$AfterPath
    )

    $beforeBitmap = [System.Drawing.Bitmap]::FromFile($BeforePath)
    $afterBitmap = [System.Drawing.Bitmap]::FromFile($AfterPath)
    try {
        $width = [Math]::Min($beforeBitmap.Width, $afterBitmap.Width)
        $height = [Math]::Min($beforeBitmap.Height, $afterBitmap.Height)
        $sampleCount = 0
        $changedCount = 0

        for ($sampleY = 0; $sampleY -lt $height; $sampleY += $SampleStep) {
            for ($sampleX = 0; $sampleX -lt $width; $sampleX += $SampleStep) {
                $beforePixel = $beforeBitmap.GetPixel($sampleX, $sampleY)
                $afterPixel = $afterBitmap.GetPixel($sampleX, $sampleY)
                $delta = ([Math]::Abs($beforePixel.R - $afterPixel.R) + [Math]::Abs($beforePixel.G - $afterPixel.G) + [Math]::Abs($beforePixel.B - $afterPixel.B)) / 3.0D
                if ($delta -ge 24) {
                    $changedCount++
                }
                $sampleCount++
            }
        }

        return [Math]::Round($changedCount / [Math]::Max(1, $sampleCount), 5)
    }
    finally {
        $beforeBitmap.Dispose()
        $afterBitmap.Dispose()
    }
}

function Compare-CaptureRegion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BeforePath,
        [Parameter(Mandatory = $true)]
        [string]$AfterPath,
        [int]$Left,
        [int]$Top,
        [int]$Width,
        [int]$Height
    )

    $beforeBitmap = [System.Drawing.Bitmap]::FromFile($BeforePath)
    $afterBitmap = [System.Drawing.Bitmap]::FromFile($AfterPath)
    try {
        $right = [Math]::Min($beforeBitmap.Width, [Math]::Min($afterBitmap.Width, $Left + $Width))
        $bottom = [Math]::Min($beforeBitmap.Height, [Math]::Min($afterBitmap.Height, $Top + $Height))
        $sampleCount = 0
        $changedCount = 0

        for ($sampleY = [Math]::Max(0, $Top); $sampleY -lt $bottom; $sampleY += $SampleStep) {
            for ($sampleX = [Math]::Max(0, $Left); $sampleX -lt $right; $sampleX += $SampleStep) {
                $beforePixel = $beforeBitmap.GetPixel($sampleX, $sampleY)
                $afterPixel = $afterBitmap.GetPixel($sampleX, $sampleY)
                $delta = ([Math]::Abs($beforePixel.R - $afterPixel.R) + [Math]::Abs($beforePixel.G - $afterPixel.G) + [Math]::Abs($beforePixel.B - $afterPixel.B)) / 3.0D
                if ($delta -ge 24) {
                    $changedCount++
                }
                $sampleCount++
            }
        }

        return [Math]::Round($changedCount / [Math]::Max(1, $sampleCount), 5)
    }
    finally {
        $beforeBitmap.Dispose()
        $afterBitmap.Dispose()
    }
}

function Measure-RedErrorCoverage {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [int]$Left,
        [int]$Top,
        [int]$Width,
        [int]$Height
    )

    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $right = [Math]::Min($bitmap.Width, $Left + $Width)
        $bottom = [Math]::Min($bitmap.Height, $Top + $Height)
        $sampleCount = 0
        $redErrorCount = 0

        for ($sampleY = [Math]::Max(0, $Top); $sampleY -lt $bottom; $sampleY += $SampleStep) {
            for ($sampleX = [Math]::Max(0, $Left); $sampleX -lt $right; $sampleX += $SampleStep) {
                $pixel = $bitmap.GetPixel($sampleX, $sampleY)
                if ($pixel.R -ge 210 -and $pixel.G -le 70 -and $pixel.B -le 70 -and ($pixel.R - $pixel.G) -ge 150 -and ($pixel.R - $pixel.B) -ge 150) {
                    $redErrorCount++
                }
                $sampleCount++
            }
        }

        return [Math]::Round($redErrorCount / [Math]::Max(1, $sampleCount), 5)
    }
    finally {
        $bitmap.Dispose()
    }
}

function New-ScaledVisibleTemplate {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath,
        [int]$Width = 64,
        [int]$Height = 64
    )

    $sourceBitmap = [System.Drawing.Bitmap]::FromFile($SourcePath)
    $scaledBitmap = New-Object System.Drawing.Bitmap $Width, $Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($scaledBitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.DrawImage($sourceBitmap, 0, 0, $Width, $Height)
        }
        finally {
            $graphics.Dispose()
        }

        $minX = $Width
        $minY = $Height
        $maxX = 0
        $maxY = 0
        for ($pixelY = 0; $pixelY -lt $Height; $pixelY++) {
            for ($pixelX = 0; $pixelX -lt $Width; $pixelX++) {
                $pixel = $scaledBitmap.GetPixel($pixelX, $pixelY)
                if ($pixel.A -gt 32) {
                    $minX = [Math]::Min($minX, $pixelX)
                    $minY = [Math]::Min($minY, $pixelY)
                    $maxX = [Math]::Max($maxX, $pixelX)
                    $maxY = [Math]::Max($maxY, $pixelY)
                }
            }
        }

        if ($minX -ge $Width -or $minY -ge $Height) {
            $scaledBitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
            return
        }

        $padding = 2
        $cropX = [Math]::Max(0, $minX - $padding)
        $cropY = [Math]::Max(0, $minY - $padding)
        $cropRight = [Math]::Min($Width - 1, $maxX + $padding)
        $cropBottom = [Math]::Min($Height - 1, $maxY + $padding)
        $cropRect = New-Object System.Drawing.Rectangle $cropX, $cropY, ($cropRight - $cropX + 1), ($cropBottom - $cropY + 1)
        $templateBitmap = $scaledBitmap.Clone($cropRect, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $templateBitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $templateBitmap.Dispose()
        }
    }
    finally {
        $sourceBitmap.Dispose()
        $scaledBitmap.Dispose()
    }
}

function New-DebugIconCompositeTemplate {
    param(
        [Parameter(Mandatory = $true)]
        [string]$IconPath,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath
    )

    $templateBitmap = New-Object System.Drawing.Bitmap 96, 96, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $iconBitmap = [System.Drawing.Bitmap]::FromFile($IconPath)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($templateBitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::FromArgb(255, 18, 28, 45))
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.DrawImage($iconBitmap, 16, 16, 64, 64)
        }
        finally {
            $graphics.Dispose()
        }
        $templateBitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $iconBitmap.Dispose()
        $templateBitmap.Dispose()
    }
}

function Find-ExpectedUiTemplate {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CapturePath,
        [Parameter(Mandatory = $true)]
        [string]$TemplatePath,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath,
        [Parameter(Mandatory = $true)]
        [hashtable]$Region
    )

    return [ResourceGameVisualTemplateMatcher]::Match(
        $CapturePath,
        $TemplatePath,
        [int]$Region.left,
        [int]$Region.top,
        [int]$Region.width,
        [int]$Region.height,
        $MinimumTemplateScore,
        $OutputPath
    )
}

function Assert-CaptureHealth {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Metrics,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    if ($Metrics.width -lt 640 -or $Metrics.height -lt 360) {
        throw "$Label capture is too small: $($Metrics.width)x$($Metrics.height)"
    }
    if ($Metrics.nonBlackCoverage -lt 0.60) {
        throw "$Label capture appears mostly black. nonBlackCoverage=$($Metrics.nonBlackCoverage)"
    }
    if ($Metrics.contrastEstimate -lt 12) {
        throw "$Label capture appears visually flat. contrastEstimate=$($Metrics.contrastEstimate)"
    }
}

if (!(Test-Path -LiteralPath $ArtifactDir)) {
    New-Item -ItemType Directory -Path $ArtifactDir | Out-Null
}

if (-not (Test-Path -LiteralPath $hostDll) -and $SkipAutomationBuild) {
    throw "Automation host DLL not found: $hostDll"
}

if (-not $SkipAutomationBuild) {
    & $dotnet build $hostProjectPath -c Debug -p:Platform=x64
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$foregroundBefore = Get-ForegroundWindowSnapshot
$clientWindow = Wait-HytaleWindow
if ($clientWindow.title -eq "Authentication Error") {
    throw "Hytale client is showing an Authentication Error window, not the playable client."
}

if (-not $SkipCommand -and $ControlMode -eq "server-file") {
    [void](Invoke-ServerControlCloseUi)
}

$beforeCapturePath = Join-Path $ArtifactDir "hytale-ui-before.png"
$beforeCapture = Capture-HytaleWindow -OutputPath $beforeCapturePath
$beforeMetrics = Measure-Capture -Path $beforeCapturePath
Assert-CaptureHealth -Metrics $beforeMetrics -Label "Before"

if (-not $SkipCommand) {
    $controlResult = Open-KingdomMenu
    if (-not $NoRestoreForeground) {
        $foregroundAfterCommand = Get-ForegroundWindowSnapshot
        if ($foregroundAfterCommand.handle -ne $foregroundBefore.handle) {
            [void](Restore-ForegroundWindow -Handle $foregroundBefore.handle)
            Start-Sleep -Milliseconds 150
        }
    }
}

$afterCapturePath = Join-Path $ArtifactDir "hytale-ui-after.png"
$afterCapture = Capture-HytaleWindow -OutputPath $afterCapturePath
$afterMetrics = Measure-Capture -Path $afterCapturePath
Assert-CaptureHealth -Metrics $afterMetrics -Label "After"
$menuLeft = [Math]::Max(0, [Math]::Floor(($afterMetrics.width - 880) / 2))
$menuTop = [Math]::Max(0, [Math]::Floor(($afterMetrics.height - 620) / 2))
$menuWidth = [Math]::Min(880, $afterMetrics.width)
$menuHeight = [Math]::Min(620, $afterMetrics.height)
$redErrorCoverage = Measure-RedErrorCoverage -Path $afterCapturePath -Left $menuLeft -Top $menuTop -Width $menuWidth -Height $menuHeight
if ($redErrorCoverage -ge 0.001) {
    throw "Resource game UI appears to contain a missing-image red X placeholder. redErrorCoverage=$redErrorCoverage afterCapture=$afterCapturePath"
}

$templateMatch = $null
$uiTemplateRegion = $null
if (-not $SkipTemplateCheck) {
    $sourceTemplatePath = Join-Path $repoRoot "src\main\resources\Common\UI\Custom\Textures\ResourceGame\icons\ui_icon_action_move.png"
    $expectedTemplatePath = Join-Path $ArtifactDir "expected-debug-icon-visible.png"
    $templateMatchPath = Join-Path $ArtifactDir "hytale-ui-template-match.png"
    $regionLeft = [Math]::Max(0, [Math]::Floor(($afterMetrics.width - 880) / 2) - 40)
    $regionTop = [Math]::Max(0, [Math]::Floor(($afterMetrics.height - 620) / 2) - 40)
    $uiTemplateRegion = @{
        left = $regionLeft
        top = $regionTop
        width = [Math]::Min(420, $afterMetrics.width - $regionLeft)
        height = [Math]::Min(300, $afterMetrics.height - $regionTop)
    }
    New-DebugIconCompositeTemplate -IconPath $sourceTemplatePath -OutputPath $expectedTemplatePath
    $templateMatch = Find-ExpectedUiTemplate -CapturePath $afterCapturePath -TemplatePath $expectedTemplatePath -OutputPath $templateMatchPath -Region $uiTemplateRegion
    if (-not $templateMatch.matched) {
        throw "Resource game debug UI icon was not detected. score=$($templateMatch.score) minimum=$MinimumTemplateScore annotatedCapture=$templateMatchPath"
    }
}

$changedCoverage = Compare-Captures -BeforePath $beforeCapturePath -AfterPath $afterCapturePath
$centerChangedCoverage = Compare-CaptureRegion `
    -BeforePath $beforeCapturePath `
    -AfterPath $afterCapturePath `
    -Left $menuLeft `
    -Top $menuTop `
    -Width $menuWidth `
    -Height $menuHeight
if (-not $SkipCommand -and -not $SkipVisualDelta -and $centerChangedCoverage -lt $MinimumCenterChangedCoverage) {
    throw "Resource game UI did not change the expected center menu region. centerChangedCoverage=$centerChangedCoverage minimum=$MinimumCenterChangedCoverage afterCapture=$afterCapturePath"
}
$overlayCoverage = [Math]::Max($afterMetrics.darkPanelCoverage, ($afterMetrics.blueAccentCoverage + $afterMetrics.goldAccentCoverage))
$uiOverlayDetected = $overlayCoverage -ge $MinimumOverlayCoverage
if (-not $uiOverlayDetected) {
    throw "Resource game UI overlay was not detected. overlayCoverage=$overlayCoverage minimum=$MinimumOverlayCoverage afterCapture=$afterCapturePath"
}

$foregroundAfter = Get-ForegroundWindowSnapshot
$summary = [ordered]@{
    artifactDir = $ArtifactDir
    clientWindow = $clientWindow
    foregroundBefore = $foregroundBefore.display
    foregroundAfter = $foregroundAfter.display
    foregroundPreserved = ($foregroundBefore.handle -eq $foregroundAfter.handle)
    foregroundRestoreAttempted = (-not $NoRestoreForeground)
    commandSent = (-not $SkipCommand)
    controlMode = $ControlMode
    controlResult = $controlResult
    openMenuCommand = if ($SkipCommand) { $null } else { $OpenMenuCommand }
    beforeCapture = $beforeCapture.outputPath
    beforeCaptureMode = $beforeCapture.captureMode
    beforeMetrics = $beforeMetrics
    afterCapture = $afterCapture.outputPath
    afterCaptureMode = $afterCapture.captureMode
    afterMetrics = $afterMetrics
    uiTemplateRegion = $uiTemplateRegion
    templateMatch = $templateMatch
    changedCoverage = $changedCoverage
    centerChangedCoverage = $centerChangedCoverage
    redErrorCoverage = $redErrorCoverage
    overlayCoverage = $overlayCoverage
    uiOverlayDetected = $uiOverlayDetected
}

$summaryJsonPath = Join-Path $ArtifactDir "hytale-ui-visual-verification.json"
$summaryTextPath = Join-Path $ArtifactDir "hytale-ui-visual-verification.txt"
$summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $summaryJsonPath -Encoding UTF8
@(
    "Hytale UI visual verification",
    "artifactDir=$ArtifactDir",
    "window=$($clientWindow.title) pid=$($clientWindow.processId)",
    "foregroundPreserved=$($summary.foregroundPreserved)",
    "foregroundRestoreAttempted=$($summary.foregroundRestoreAttempted)",
    "commandSent=$($summary.commandSent) controlMode=$ControlMode command=$OpenMenuCommand",
    "beforeCapture=$($beforeCapture.outputPath) mode=$($beforeCapture.captureMode)",
    "afterCapture=$($afterCapture.outputPath) mode=$($afterCapture.captureMode)",
    "afterSize=$($afterMetrics.width)x$($afterMetrics.height)",
    "darkPanelCoverage=$($afterMetrics.darkPanelCoverage)",
    "blueAccentCoverage=$($afterMetrics.blueAccentCoverage)",
    "goldAccentCoverage=$($afterMetrics.goldAccentCoverage)",
    "templateMatched=$(-not $SkipTemplateCheck -and $templateMatch.Matched)",
    "templateScore=$(if ($templateMatch) { $templateMatch.Score } else { '' })",
    "templateOutput=$(if ($templateMatch) { $templateMatch.OutputPath } else { '' })",
    "changedCoverage=$changedCoverage",
    "centerChangedCoverage=$centerChangedCoverage",
    "redErrorCoverage=$redErrorCoverage",
    "uiOverlayDetected=$uiOverlayDetected"
) | Set-Content -LiteralPath $summaryTextPath -Encoding UTF8

[PSCustomObject]$summary | Format-List
