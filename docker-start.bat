@echo off
chcp 65001 >nul
echo ================================================================
echo   🐋 DOCKER - Start SQL Server for SmartNotebook
echo ================================================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Docker is not installed!
    echo.
    echo Please install Docker Desktop:
    echo https://www.docker.com/products/docker-desktop/
    echo.
    pause
    exit /b 1
)

echo ✅ Docker detected
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Docker is not running!
    echo.
    echo Please start Docker Desktop first.
    echo.
    pause
    exit /b 1
)

echo ✅ Docker is running
echo.

echo 🚀 Starting SQL Server container...
echo.

REM Start SQL Server with docker-compose
docker-compose up -d sqlserver

if errorlevel 1 (
    echo.
    echo ❌ Failed to start SQL Server container
    pause
    exit /b 1
)

echo.
echo ⏳ Waiting for SQL Server to be ready...
timeout /t 10 /nobreak >nul

REM Wait for health check
:WAIT_LOOP
docker-compose ps sqlserver | find "healthy" >nul
if errorlevel 1 (
    echo    Still initializing...
    timeout /t 5 /nobreak >nul
    goto WAIT_LOOP
)

echo.
echo ================================================================
echo   ✅ SQL Server is ready!
echo ================================================================
echo.
echo   Container: smartnotebook-db
echo   Host: localhost
echo   Port: 1433
echo   Username: sa
echo   Password: SmartNotebook@2024
echo.
echo 🎯 You can now run SmartNotebook application!
echo.
echo To stop SQL Server: docker-stop.bat
echo.
pause
