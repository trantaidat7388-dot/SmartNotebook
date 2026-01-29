@echo off
chcp 65001 >nul
echo ================================================================
echo   🔌 SMART NOTEBOOK - Offline Build
echo ================================================================
echo.
echo Building từ source code SỬ DỤNG LOCAL REPOSITORY
echo KHÔNG CẦN INTERNET!
echo.

REM Check Maven installed
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven chưa được cài đặt!
    echo.
    echo Vui lòng cài Maven từ: https://maven.apache.org/download.cgi
    echo Hoặc sử dụng mvnw.cmd (Maven Wrapper) đi kèm
    echo.
    pause
    exit /b 1
)

echo [INFO] Sử dụng Maven local repository: maven-repository/
echo [INFO] Build mode: OFFLINE (không download từ internet)
echo.

REM Build với local repository
echo [1/2] Cleaning previous builds...
call mvn clean -o -Dmaven.repo.local=maven-repository

echo.
echo [2/2] Building application with local dependencies...
call mvn package -o -Dmaven.repo.local=maven-repository

if %errorlevel% neq 0 (
    echo.
    echo ================================================================
    echo   ❌ BUILD FAILED!
    echo ================================================================
    echo.
    echo Có thể nguyên nhân:
    echo   1. Maven repository không đầy đủ dependencies
    echo   2. Source code có lỗi
    echo.
    echo Kiểm tra output ở trên để biết chi tiết.
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ BUILD THÀNH CÔNG (OFFLINE MODE)
echo ================================================================
echo.
echo 📦 Các file đã được tạo:
echo   • target\SmartNotebook.jar (Fat JAR)
echo   • target\SmartNotebook-1.0-SNAPSHOT-distribution.zip
echo.
echo 🚀 Chạy ứng dụng:
echo   java -jar target\SmartNotebook.jar
echo.
echo ================================================================
pause
