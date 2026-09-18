# =============================================================================
# IgnitionAI 2.0 - Launch Script
# Compiles and launches the Windows Swing desktop app
# Builds the Android APK (requires Android SDK)
# =============================================================================
# Usage:
#   .\launch.ps1                  # Build + launch desktop app AND build Android APK
#   .\launch.ps1 -DesktopOnly    # Only compile and launch the Windows desktop app
#   .\launch.ps1 -AndroidOnly    # Only build the Android APK
#   .\launch.ps1 -Help           # Show help
# =============================================================================

param (
    [switch]$DesktopOnly,
    [switch]$AndroidOnly,
    [switch]$Help
)

$ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path

# Colour helpers
function Print-Header { param($msg) Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Print-OK     { param($msg) Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Print-Warn   { param($msg) Write-Host "  [!!]  $msg" -ForegroundColor Yellow }
function Print-Err    { param($msg) Write-Host "  [XX]  $msg" -ForegroundColor Red }
function Print-Info   { param($msg) Write-Host "        $msg" -ForegroundColor Gray }

Write-Host ""
Write-Host "  IgnitionAI 2.0 - Dealership Inspection Platform" -ForegroundColor Magenta
Write-Host "  Windows Desktop App  +  Android APK Builder" -ForegroundColor Magenta
Write-Host ""

if ($Help) {
    Write-Host "Usage:"
    Write-Host "  .\launch.ps1                  # Launch desktop app AND build APK"
    Write-Host "  .\launch.ps1 -DesktopOnly     # Launch desktop app only"
    Write-Host "  .\launch.ps1 -AndroidOnly     # Build APK only"
    Write-Host "  .\launch.ps1 -Help            # Show this help"
    exit 0
}

# =============================================================================
# SECTION 1: Windows Desktop App (pure Swing - no external deps)
# =============================================================================
function Build-And-Launch-Desktop {
    Print-Header "Windows Desktop App"

    # Find javac / java
    $javac = $null
    $java  = $null

    if ($env:JAVA_HOME) {
        $javac = Join-Path $env:JAVA_HOME "bin\javac.exe"
        $java  = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (-not (Test-Path $javac)) { $javac = $null; $java = $null }
    }
    if (-not $javac) {
        $javac = (Get-Command javac -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
        $java  = (Get-Command java  -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
    }
    if (-not $javac) {
        Print-Err "Java JDK not found. Install JDK 17+ and set JAVA_HOME."
        Print-Info "Download: https://adoptium.net/"
        return $false
    }

    $verLine = (& $java -version 2>&1) | Select-String "version" | Select-Object -First 1
    Print-OK  "Java: $verLine"

    # Paths
    $srcFile = Join-Path $ROOT "desktop-app\src\main\java\ai\ignition\desktop\DealershipDesktopApp.java"
    $outDir  = Join-Path $ROOT "desktop-app\build\classes"

    if (-not (Test-Path $srcFile)) {
        Print-Err "Source not found: $srcFile"
        return $false
    }

    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

    # Compile
    Print-Info "Compiling DealershipDesktopApp.java ..."
    $result = & $javac -d $outDir $srcFile 2>&1
    if ($LASTEXITCODE -ne 0) {
        Print-Err "Compilation failed:"
        $result | ForEach-Object { Print-Info $_ }
        return $false
    }
    Print-OK "Compiled -> $outDir"

    # Launch in new window (non-blocking)
    Print-Info "Starting GUI ..."
    Start-Process -FilePath $java `
        -ArgumentList @("-cp", "`"$outDir`"", "ai.ignition.desktop.DealershipDesktopApp") `
        -WindowStyle Normal

    Print-OK "Desktop application launched!"
    return $true
}

# =============================================================================
# SECTION 2: Android APK
# =============================================================================
function Build-Android-APK {
    Print-Header "Android APK"

    $androidAppDir = Join-Path $ROOT "android-app"

    # Find Android SDK
    $sdkRoot = $env:ANDROID_HOME
    if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
    if (-not $sdkRoot) {
        $candidates = @(
            "$env:LOCALAPPDATA\Android\Sdk",
            "$env:USERPROFILE\AppData\Local\Android\Sdk",
            "C:\Android\Sdk",
            "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
        )
        foreach ($c in $candidates) {
            if (Test-Path $c) { $sdkRoot = $c; break }
        }
    }

    if (-not $sdkRoot -or -not (Test-Path $sdkRoot)) {
        Print-Err "Android SDK not found."
        Print-Info "Install Android Studio: https://developer.android.com/studio"
        Print-Info "Then set ANDROID_HOME to your SDK path, e.g.:"
        Print-Info "  [Environment]::SetEnvironmentVariable('ANDROID_HOME','C:\Users\YourName\AppData\Local\Android\Sdk','User')"
        return $false
    }
    Print-OK "Android SDK: $sdkRoot"

    # Write local.properties
    $fwdSdk = $sdkRoot -replace "\\", "/"
    Set-Content -Path (Join-Path $androidAppDir "local.properties") -Value "sdk.dir=$fwdSdk"
    Print-OK "local.properties written."

    # Locate or create gradlew
    $gradlew = Join-Path $androidAppDir "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        # Try system gradle to generate wrapper
        $sysGradle = (Get-Command gradle -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
        if ($sysGradle) {
            Print-Info "Generating Gradle wrapper via system gradle ..."
            Push-Location $androidAppDir
            & $sysGradle wrapper --gradle-version 8.4 --distribution-type bin 2>&1 | Out-Null
            Pop-Location
        }
    }

    if (-not (Test-Path $gradlew)) {
        # Create minimal wrapper manually
        Print-Warn "No gradlew found. Creating minimal Gradle wrapper ..."
        $wrapperDir = Join-Path $androidAppDir "gradle\wrapper"
        New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null

        @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@ | Set-Content "$wrapperDir\gradle-wrapper.properties"

        @'
@if "%DEBUG%"=="" @echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java.exe
)
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
    echo ERROR: gradle-wrapper.jar missing. Run: gradle wrapper --gradle-version 8.4
    exit /B 1
)
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
'@ | Set-Content (Join-Path $androidAppDir "gradlew.bat")

        # Download gradle-wrapper.jar
        $jarDest = "$wrapperDir\gradle-wrapper.jar"
        if (-not (Test-Path $jarDest)) {
            Print-Info "Downloading gradle-wrapper.jar ..."
            try {
                Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" `
                    -OutFile $jarDest -UseBasicParsing -ErrorAction Stop
                Print-OK "gradle-wrapper.jar downloaded."
            } catch {
                Print-Warn "Could not download gradle-wrapper.jar automatically."
                Print-Info "Place it manually in: $wrapperDir"
            }
        }
    }

    if (-not (Test-Path $gradlew)) {
        Print-Err "Could not create Gradle wrapper. Install Gradle 8.x: https://gradle.org/install/"
        return $false
    }
    Print-OK "Gradle wrapper: $gradlew"

    # Build APK
    Print-Info "Running assembleDebug (first run downloads Gradle + dependencies) ..."
    Push-Location $androidAppDir
    $out  = & $gradlew assembleDebug 2>&1
    $exit = $LASTEXITCODE
    Pop-Location

    if ($exit -ne 0) {
        Print-Err "APK build FAILED (exit $exit). Last 30 lines:"
        $out | Select-Object -Last 30 | ForEach-Object { Print-Info $_ }
        return $false
    }

    # Find and copy APK
    $apk = Get-ChildItem -Path "$androidAppDir\build\outputs\apk\debug" `
                         -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue |
           Select-Object -First 1

    if ($apk) {
        $dest = Join-Path $ROOT "IgnitionAI-debug.apk"
        Copy-Item -Path $apk.FullName -Destination $dest -Force
        Print-OK "APK built:  $($apk.FullName)"
        Print-OK "Copied to:  $dest"
    } else {
        Print-Warn "Build succeeded but APK not found in expected location."
        Print-Info "Look in: $androidAppDir\build\outputs\apk\"
    }
    return $true
}

# =============================================================================
# MAIN
# =============================================================================
$desktopOk = $true
$androidOk = $true

if (-not $AndroidOnly)  { $desktopOk = Build-And-Launch-Desktop }
if (-not $DesktopOnly)  { $androidOk = Build-Android-APK }

Write-Host ""
Write-Host "================================================" -ForegroundColor DarkGray
Write-Host "  RESULTS" -ForegroundColor White
if (-not $AndroidOnly) {
    if ($desktopOk) { Print-OK "Desktop App  -> Launched" }
    else            { Print-Err "Desktop App  -> Failed" }
}
if (-not $DesktopOnly) {
    if ($androidOk) { Print-OK "Android APK  -> IgnitionAI-debug.apk" }
    else            { Print-Err "Android APK  -> Failed" }
}
Write-Host "================================================" -ForegroundColor DarkGray
Write-Host ""
