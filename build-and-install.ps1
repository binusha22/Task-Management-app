# ==============================================================================
# GMS Task Management - Build, Install & Launch Script
# ==============================================================================
# Usage:
#   .\build-and-install.ps1              # Build APK, install to phone, and launch
#   .\build-and-install.ps1 -BuildOnly   # Only build the debug APK
# ==============================================================================

param (
    [switch]$BuildOnly
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  GMS Task Management - Android Build and Test Workflow   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# --- Paths ------------------------------------------------------------------
$ProjectRoot = $PSScriptRoot
if (-not $ProjectRoot) { $ProjectRoot = (Get-Location).Path }

$Gradlew     = Join-Path $ProjectRoot "gradlew.bat"
$ApkPath     = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$PackageName = "com.gms.taskmanagement"
$MainClass   = "com.gms.taskmanagement.splash.SplashActivity"

if (-not (Test-Path $Gradlew)) {
    Write-Host "[ERROR] gradlew.bat not found in: $ProjectRoot" -ForegroundColor Red
    exit 1
}

# --- Resolve ADB ------------------------------------------------------------
$AdbCmd = "adb"
if (-not (Get-Command "adb" -ErrorAction SilentlyContinue)) {
    $SdkDir    = $null
    $LocalProps = Join-Path $ProjectRoot "local.properties"

    if (Test-Path $LocalProps) {
        $PropLine = Get-Content $LocalProps | Where-Object { $_ -like "sdk.dir*" } | Select-Object -First 1
        if ($PropLine) {
            $SdkDir = ($PropLine -split "=", 2)[1].Trim().Replace("\\:", ":").Replace("\\", "\")
        }
    }

    if (-not $SdkDir -and $env:ANDROID_HOME)     { $SdkDir = $env:ANDROID_HOME }
    if (-not $SdkDir -and $env:ANDROID_SDK_ROOT) { $SdkDir = $env:ANDROID_SDK_ROOT }
    if (-not $SdkDir) { $SdkDir = "$env:LOCALAPPDATA\Android\Sdk" }

    $CandidateAdb = Join-Path $SdkDir "platform-tools\adb.exe"
    if (Test-Path $CandidateAdb) { $AdbCmd = $CandidateAdb }
}

# --- Step 1: Build ----------------------------------------------------------
Write-Host ">>> [1/3] Building Debug APK with Gradle..." -ForegroundColor Yellow
$BuildStart = Get-Date

& $Gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[BUILD FAILED] Gradle exited with code $LASTEXITCODE." -ForegroundColor Red
    Write-Host "Fix the errors above and try again." -ForegroundColor Red
    exit 1
}

$BuildSecs = [math]::Round(((Get-Date) - $BuildStart).TotalSeconds, 1)
Write-Host "Build succeeded in $BuildSecs seconds." -ForegroundColor Green

if (-not (Test-Path $ApkPath)) {
    Write-Host "[ERROR] APK not found at: $ApkPath" -ForegroundColor Red
    exit 1
}

# Compute APK size without using the 1MB PS literal (avoids parser confusion)
$ApkBytes = (Get-Item $ApkPath).Length
$ApkMB    = [math]::Round($ApkBytes / 1048576, 2)
$ApkInfo  = "APK: " + $ApkPath + " (" + $ApkMB + " MB)"
Write-Host $ApkInfo -ForegroundColor Gray

if ($BuildOnly) {
    Write-Host ""
    Write-Host "Build-only mode. APK is ready. Skipping install." -ForegroundColor Green
    exit 0
}

# --- Step 2: Check Device ---------------------------------------------------
Write-Host ""
Write-Host ">>> [2/3] Checking connected Android device via ADB..." -ForegroundColor Yellow

$AdbOut      = & $AdbCmd devices 2>&1
$DeviceLines = $AdbOut | Where-Object { $_ -and ($_ -notlike "List of devices*") -and ($_.Trim() -ne "") }

$AuthorizedList   = New-Object System.Collections.Generic.List[string]
$UnauthorizedList = New-Object System.Collections.Generic.List[string]

foreach ($line in $DeviceLines) {
    $parts = $line.Trim() -split "\s+"
    if ($parts.Count -ge 2) {
        $serial = $parts[0]
        $status = $parts[1]
        if ($status -eq "device") {
            $AuthorizedList.Add($serial)
        } elseif ($status -eq "unauthorized") {
            $UnauthorizedList.Add($serial)
        }
    }
}

if ($UnauthorizedList.Count -gt 0 -and $AuthorizedList.Count -eq 0) {
    Write-Host ""
    Write-Host "[UNAUTHORIZED] Phone detected but not authorized!" -ForegroundColor Red
    Write-Host "On your phone: tap ALLOW on the USB Debugging popup, then run this script again." -ForegroundColor Yellow
    exit 1
}

if ($AuthorizedList.Count -eq 0) {
    Write-Host ""
    Write-Host "[NO DEVICE] No authorized Android phone found." -ForegroundColor Red
    Write-Host "Check:" -ForegroundColor Yellow
    Write-Host "  1. USB cable is connected (use a data cable, not charge-only)" -ForegroundColor Yellow
    Write-Host "  2. Developer Options is enabled on your phone" -ForegroundColor Yellow
    Write-Host "  3. USB Debugging is turned ON" -ForegroundColor Yellow
    Write-Host "  4. USB mode is File Transfer (MTP), not Charging only" -ForegroundColor Yellow
    Write-Host "  5. Run: adb devices   to verify detection" -ForegroundColor Yellow
    exit 1
}

$TargetDevice = $AuthorizedList[0]
Write-Host "Connected device: $TargetDevice" -ForegroundColor Green

# --- Step 3: Install --------------------------------------------------------
Write-Host ""
Write-Host ">>> [3/3] Installing APK on $TargetDevice ..." -ForegroundColor Yellow

& $AdbCmd -s $TargetDevice install -r "$ApkPath"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[INSTALL FAILED] ADB could not install the APK." -ForegroundColor Red
    Write-Host "Check your phone screen for a security or permission popup." -ForegroundColor Yellow
    exit 1
}

Write-Host "APK installed successfully." -ForegroundColor Green

# --- Launch -----------------------------------------------------------------
Write-Host "Launching app on device..." -ForegroundColor Yellow
& $AdbCmd -s $TargetDevice shell am start -n "$PackageName/$MainClass" | Out-Null

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  SUCCESS! App is running on your phone. Ready to test!   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
