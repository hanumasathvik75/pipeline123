@echo off
setlocal
title IgnitionAI Desktop App Launcher
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-IgnitionAI-Desktop.ps1"
if errorlevel 1 pause
endlocal
