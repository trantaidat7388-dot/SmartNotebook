@echo off
chcp 65001 >nul
echo ================================================================
echo   🐋 DOCKER - Stop SmartNotebook Containers
echo ================================================================
echo.

echo 🛑 Stopping all containers...
echo.

docker-compose down

if errorlevel 1 (
    echo.
    echo ❌ Failed to stop containers
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ All containers stopped!
echo ================================================================
echo.
echo To remove data volumes as well, run:
echo   docker-compose down -v
echo.
echo To start again: docker-start.bat
echo.
pause
