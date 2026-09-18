param([string]$Jpackage = 'jpackage', [string]$OutputName = 'IgnitionAI-Preview')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat :native-desktop:jar :native-core:jar --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Java build failed' }
    $staging = Join-Path $PSScriptRoot 'build/windows-input'
    New-Item -ItemType Directory -Force $staging | Out-Null
    Copy-Item native-desktop/build/libs/ignitionai-desktop.jar $staging -Force
    Copy-Item native-core/build/libs/native-core.jar $staging -Force
    Copy-Item virtual-sensors/src/main/resources/sensors $staging -Recurse -Force
    if (Test-Path (Join-Path $PSScriptRoot "releases/$OutputName")) { throw 'Output exists. Supply a new OutputName.' }
    $packageTool = (Get-Command $Jpackage -ErrorAction Stop).Source
    $linkTool = Join-Path (Split-Path $packageTool) 'jlink.exe'
    $runtime = Join-Path $PSScriptRoot "build/runtime-$OutputName"
    if (Test-Path $runtime) { throw 'Runtime staging exists. Supply a new OutputName.' }
    & $linkTool --add-modules java.base,java.desktop,java.logging,java.xml --strip-debug --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) { throw 'Runtime creation failed' }
    & $packageTool --type app-image --name $OutputName --input $staging --main-jar ignitionai-desktop.jar --main-class com.ignitionai.desktop.IgnitionDesktop --dest releases --app-version 0.1.0 --vendor IgnitionAI --runtime-image $runtime
    if ($LASTEXITCODE -ne 0) { throw 'Windows packaging failed' }
    Write-Output "Built releases/$OutputName/$OutputName.exe with its Java runtime."
} finally { Pop-Location }
