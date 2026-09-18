param(
    [string]$ReleaseName = "IgnitionAI-preview-$(Get-Date -Format yyyyMMdd-HHmm)"
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $androidJava = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path (Join-Path $androidJava 'bin/java.exe')) { $env:JAVA_HOME = $androidJava }
    Write-Host "== Java verification ==" -ForegroundColor Cyan
    cmd /c build_and_test.bat
    if ($LASTEXITCODE -ne 0) { throw "Java verification failed" }

    Write-Host "== Android APK ==" -ForegroundColor Cyan
    .\gradlew.bat :android-app:assembleDebug :android-app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Android build failed" }

    Write-Host "== Windows package ==" -ForegroundColor Cyan
    powershell -NoProfile -ExecutionPolicy Bypass -File .\package-windows.ps1 -OutputName $ReleaseName
    if ($LASTEXITCODE -ne 0) { throw "Windows packaging failed" }

    $releaseDir = Join-Path $PSScriptRoot 'releases'
    $windowsDir = Join-Path $releaseDir $ReleaseName
    $windowsZip = Join-Path $releaseDir "$ReleaseName-windows.zip"
    $androidApk = Join-Path $releaseDir "$ReleaseName-android.apk"
    $checksums = Join-Path $releaseDir "$ReleaseName-SHA256SUMS.txt"
    Compress-Archive -LiteralPath $windowsDir -DestinationPath $windowsZip -Force
    Copy-Item (Join-Path $PSScriptRoot 'android-app/build/outputs/apk/debug/android-app-debug.apk') $androidApk -Force
    Get-FileHash $windowsZip,$androidApk -Algorithm SHA256 |
        ForEach-Object { "$($_.Hash.ToLowerInvariant())  $([IO.Path]::GetFileName($_.Path))" } |
        Set-Content -Encoding ascii $checksums

    Write-Host "RELEASE READY: $ReleaseName" -ForegroundColor Green
    Get-Item $windowsZip,$androidApk,$checksums | Select-Object Name,Length
} finally { Pop-Location }
