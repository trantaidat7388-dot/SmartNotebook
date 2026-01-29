@echo off
chcp 65001 >nul
echo ================================================================
echo   🐋 DOCKER - Setup Database Schema
echo ================================================================
echo.

REM Check if SQL Server container is running
docker ps | find "smartnotebook-db" >nul
if errorlevel 1 (
    echo ❌ ERROR: SQL Server container is not running!
    echo.
    echo Please run docker-start.bat first.
    echo.
    pause
    exit /b 1
)

echo ✅ SQL Server container is running
echo.

echo 📝 Importing database schema...
echo.

REM Run db-init container to setup database
docker-compose up db-init

if errorlevel 1 (
    echo.
    echo ❌ Failed to initialize database
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ Database schema imported successfully!
echo ================================================================
echo.
echo Database 'SmartNotebook' is ready to use.
echo.
pause
