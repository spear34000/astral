# PRoot & Alpine Rootfs Download Script for Windows
# PowerShell script to download PRoot binary and Alpine Linux rootfs

param(
    [switch]$Force = $false
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AssetsDir = $ScriptDir

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Astral Node.js Runtime Setup" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# URLs
$ProotVersion = "5.3.1"
$ProotArm64Url = "https://raw.githubusercontent.com/foxytouxxx/freeroot/main/proot-aarch64"

$AlpineVersion = "3.12"
$AlpineMinor = "12"
$AlpineArch = "aarch64"
$AlpineUrl = "https://dl-cdn.alpinelinux.org/alpine/v$AlpineVersion/releases/$AlpineArch/alpine-minirootfs-$AlpineVersion.$AlpineMinor-$AlpineArch.tar.gz"

# Download PRoot
Write-Host "[1/4] Downloading PRoot binary..." -ForegroundColor Yellow
$ProotPath = Join-Path $AssetsDir "proot-arm64"
if ((Test-Path $ProotPath) -and -not $Force) {
    Write-Host "✓ PRoot binary already exists" -ForegroundColor Green
} else {
    try {
        Write-Host "Downloading from: $ProotArm64Url"
        Invoke-WebRequest -Uri $ProotArm64Url -OutFile $ProotPath -UseBasicParsing
        Write-Host "✓ PRoot binary downloaded" -ForegroundColor Green
    } catch {
        Write-Host "✗ Failed to download PRoot binary" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host "Please download manually from: https://github.com/termux/proot/releases"
        exit 1
    }
}

# Download Alpine rootfs
Write-Host ""
Write-Host "[2/4] Downloading Alpine Linux rootfs..." -ForegroundColor Yellow
$AlpineTarGz = Join-Path $AssetsDir "alpine-minirootfs.tar.gz"
if ((Test-Path $AlpineTarGz) -and -not $Force) {
    Write-Host "✓ Alpine rootfs already downloaded" -ForegroundColor Green
} else {
    try {
        Write-Host "Downloading from: $AlpineUrl"
        Write-Host "(This may take a few minutes...)"
        Invoke-WebRequest -Uri $AlpineUrl -OutFile $AlpineTarGz -UseBasicParsing
        Write-Host "✓ Alpine rootfs downloaded" -ForegroundColor Green
    } catch {
        Write-Host "✗ Failed to download Alpine rootfs" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host "Please download manually from: https://alpinelinux.org/downloads/"
        exit 1
    }
}

# Extract and recompress
Write-Host ""
Write-Host "[3/4] Extracting and recompressing to tar.xz..." -ForegroundColor Yellow
$AlpineTarXz = Join-Path $AssetsDir "alpine-rootfs.tar.xz"

if ((Test-Path $AlpineTarXz) -and -not $Force) {
    Write-Host "✓ alpine-rootfs.tar.xz already exists" -ForegroundColor Green
} else {
    Write-Host "⚠ Recompression requires 7-Zip or WSL" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Option 1: Using 7-Zip (Windows)"
    Write-Host "  1. Install 7-Zip from https://www.7-zip.org/"
    Write-Host "  2. Extract alpine-minirootfs.tar.gz"
    Write-Host "  3. Compress to alpine-rootfs.tar.xz"
    Write-Host ""
    Write-Host "Option 2: Using WSL (Recommended)"
    Write-Host "  wsl bash -c 'cd ""$AssetsDir"" && tar -xzf alpine-minirootfs.tar.gz -C /tmp/alpine && tar -cJf alpine-rootfs.tar.xz -C /tmp/alpine .'"
    Write-Host ""
    
    # Try WSL if available
    $wslAvailable = $null -ne (Get-Command wsl -ErrorAction SilentlyContinue)
    if ($wslAvailable) {
        Write-Host "Attempting to use WSL..." -ForegroundColor Cyan
        try {
            $TmpDir = "/tmp/alpine-$(Get-Random)"
            wsl bash -c "mkdir -p $TmpDir && tar -xzf '$($AlpineTarGz -replace '\\', '/')' -C $TmpDir && tar -cJf '$($AlpineTarXz -replace '\\', '/')' -C $TmpDir . && rm -rf $TmpDir"
            Write-Host "✓ Rootfs recompressed successfully" -ForegroundColor Green
        } catch {
            Write-Host "✗ WSL compression failed: $_" -ForegroundColor Red
            Write-Host "Please compress manually" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠ WSL not detected. Please compress manually." -ForegroundColor Yellow
    }
}

# Cleanup
Write-Host ""
Write-Host "[4/4] Cleanup..." -ForegroundColor Yellow
if (Test-Path $AlpineTarGz) {
    Remove-Item $AlpineTarGz -Force
    Write-Host "✓ Removed temporary tar.gz file" -ForegroundColor Green
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $ProotPath) {
    $ProotSize = (Get-Item $ProotPath).Length / 1MB
    Write-Host "  - proot-arm64 ($([math]::Round($ProotSize, 2)) MB)"
}
if (Test-Path $AlpineTarXz) {
    $RootfsSize = (Get-Item $AlpineTarXz).Length / 1MB
    Write-Host "  - alpine-rootfs.tar.xz ($([math]::Round($RootfsSize, 2)) MB)"
} else {
    Write-Host "  ⚠ alpine-rootfs.tar.xz not created - please create manually" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "You can now build the APK with:" -ForegroundColor Cyan
Write-Host "  cd ..\..\..\..\..\..\.." -ForegroundColor Gray
Write-Host "  .\gradlew.bat assembleDebug" -ForegroundColor Gray
Write-Host ""
