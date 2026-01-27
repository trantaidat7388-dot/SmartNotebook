@echo off
chcp 65001 >nul
echo ================================================
echo   📓 SMART NOTEBOOK - Khởi động ứng dụng
echo ================================================
echo.
echo Starting JavaFX Application...
echo.

REM Run the application
call mvn javafx:run

REM If error
if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khởi động!
    echo.
    echo 🔧 Troubleshooting:
    echo   1. Chạy 'setup.bat' nếu chưa setup
    echo   2. Kiểm tra 'config.properties' (database)
    echo   3. Kiểm tra SQL Server đang chạy
    echo.
    pause
    exit /b 1
)
