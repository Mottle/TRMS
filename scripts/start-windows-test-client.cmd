@echo off
setlocal

rem One-click Windows entry point for the PowerShell-based NeoForge test client.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-windows-test-client.ps1"
exit /b %ERRORLEVEL%
