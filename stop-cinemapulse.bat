@echo off
title Stop CinemaPulse
color 0C
echo ===================================================
echo           Stopping CinemaPulse Servers
echo ===================================================
echo.

powershell -Command "$p8080 = (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue).OwningProcess; if ($p8080) { Stop-Process -Id $p8080 -Force -ErrorAction SilentlyContinue; Write-Host 'Stopped Backend on port 8080' } else { Write-Host 'Backend is not running' }; $p3000 = (Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue).OwningProcess; if ($p3000) { Stop-Process -Id $p3000 -Force -ErrorAction SilentlyContinue; Write-Host 'Stopped Frontend on port 3000' } else { Write-Host 'Frontend is not running' }"

echo.
echo ===================================================
echo           CinemaPulse Stopped Cleanly
echo ===================================================
timeout /t 3 >nul
exit
