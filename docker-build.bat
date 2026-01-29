@echo off
chcp 65001 >nul
echo ================================================================
echo   🐋 DOCKER - Build SmartNotebook Image
echo ================================================================
echo.

echo 🔨 Building Docker image...
echo.

docker build -t smartnotebook:latest .

if errorlevel 1 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ Docker image built successfully!
echo ================================================================
echo.
echo Image: smartnotebook:latest
echo.
echo To run in container (not recommended for JavaFX):
echo   docker run -it --network smartnotebook-network smartnotebook:latest
echo.
pause
