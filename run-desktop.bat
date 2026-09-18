@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File compile.ps1
if errorlevel 1 exit /b 1
javaw -cp out com.ignitionai.desktop.IgnitionDesktop
