@echo off
chcp 65001 >nul
echo ================================================================
echo   📦 BUILD SOURCE DISTRIBUTION - Offline Build Package
echo ================================================================
echo.
echo Tạo gói SOURCE CODE + MAVEN REPO để build hoàn toàn offline
echo.

echo [1/4] Bundling Maven repository...
echo.
call bundle-maven-repo.bat

if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khi bundle Maven repository!
    pause
    exit /b 1
)

echo.
echo [2/4] Building application...
echo.
call mvn clean package

if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khi build application!
    pause
    exit /b 1
)

echo.
echo [3/4] Creating source distribution package...
echo.
call mvn assembly:single -P source-distribution

if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khi tạo source distribution!
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ SOURCE DISTRIBUTION PACKAGE CREATED!
echo ================================================================
echo.
echo 📦 File đã tạo:
echo   target\SmartNotebook-1.0-SNAPSHOT-source.zip
echo.
echo 📂 Package chứa:
echo   • Source code (src/)
echo   • Maven repository (maven-repository/)
echo   • Build scripts (build-offline.bat)
echo   • Documentation (README, PACKAGING, etc.)
echo   • Database scripts (database.sql)
echo.
echo 📋 Cách sử dụng:
echo   1. Giải nén ZIP file
echo   2. Chạy: build-offline.bat
echo   3. Build HOÀN TOÀN OFFLINE, không cần internet!
echo.
echo 💾 File size:
dir /s target\SmartNotebook-1.0-SNAPSHOT-source.zip | find "SmartNotebook"
echo.
echo ================================================================
pause
