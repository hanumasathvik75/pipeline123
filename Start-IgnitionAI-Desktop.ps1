<#
Starts the native IgnitionAI PySide6 desktop application with 1 click.
Automatically installs missing dependencies if needed.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSCommandPath
Set-Location -LiteralPath $projectRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "                IgnitionAI Desktop App                      " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

try {
    $python = Get-Command python -ErrorAction Stop
    $pyVer = & $python.Source --version 2>&1
    Write-Host "Using Python: $pyVer" -ForegroundColor Green

    # Check dependencies
    Write-Host "Checking desktop dependencies..." -ForegroundColor Gray
    & $python.Source -c "import PySide6, matplotlib, numpy, pandas, reportlab" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Installing missing dependencies (first run only)..." -ForegroundColor Yellow
        $reqPath = Join-Path $projectRoot "ignition-ai-schema\desktop\requirements.txt"
        if (-not (Test-Path $reqPath)) {
            $reqPath = Join-Path $projectRoot "desktop\requirements.txt"
        }
        & $python.Source -m pip install --disable-pip-version-check -r $reqPath
        if ($LASTEXITCODE -ne 0) { throw "Failed to install dependencies." }
    }

    # Determine desktop main script location
    $mainScript = Join-Path $projectRoot "ignition-ai-schema\desktop\main.py"
    if (-not (Test-Path $mainScript)) {
        $mainScript = Join-Path $projectRoot "desktop\main.py"
    }

    Write-Host "Launching IgnitionAI Desktop App..." -ForegroundColor Green
    & $python.Source $mainScript
}
catch {
    Write-Host "Failed to launch IgnitionAI Desktop App: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Press Enter to close"
    exit 1
}
