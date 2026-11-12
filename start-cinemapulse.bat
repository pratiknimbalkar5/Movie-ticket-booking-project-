@echo off
title CinemaPulse Launcher
color 0A
echo ===================================================
echo           Starting CinemaPulse System
echo ===================================================
echo.

cd /d "%~dp0"

echo [1/3] Starting Spring Boot Backend (Port 8080)...
start "CinemaPulse Backend" cmd /k "cd /d "%~dp0backend" && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.moviebooking.MovieBookingApplication""

echo [2/3] Starting React Frontend (Port 3000)...
start "CinemaPulse Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo [3/3] Waiting for servers to initialize...
timeout /t 5 /nobreak >nul

echo Opening CinemaPulse in your browser...
start http://localhost:3000

echo.
echo ===================================================
echo   CinemaPulse is now running!
echo   Frontend : http://localhost:3000
echo   Backend  : http://localhost:8080
echo.
echo   Keep the two command windows open while using.
echo   To stop the servers, run 'stop-cinemapulse.bat'
echo ===================================================
timeout /t 5 >nul
exit
