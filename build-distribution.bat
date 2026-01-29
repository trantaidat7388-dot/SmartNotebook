@echo off
chcp 65001 >nul
echo ================================================================
echo   📦 SMART NOTEBOOK - XÂY DỰNG GÓI PHÂN PHỐI ĐẦY ĐỦ
echo ================================================================
echo.

echo [1/2] Đang build Fat JAR với tất cả dependencies...
echo.
call mvn clean package

if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khi build! Kiểm tra lại Maven và dependencies.
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   ✅ BUILD THÀNH CÔNG!
echo ================================================================
echo.
echo 📦 Các file đã được tạo:
echo.
echo   1. Fat JAR (chứa tất cả thư viện):
echo      • target\SmartNotebook.jar
echo.
echo   2. Gói phân phối đầy đủ (ZIP):
echo      • target\SmartNotebook-1.0-SNAPSHOT-distribution.zip
echo.
echo 📂 Cấu trúc gói phân phối:
echo.
echo   SmartNotebook-1.0-SNAPSHOT\
echo   ├── SmartNotebook.jar          (Ứng dụng chính)
echo   ├── run.bat                    (Script chạy ứng dụng)
echo   ├── setup.bat                  (Script setup)
echo   ├── INSTALL.txt                (Hướng dẫn cài đặt)
echo   ├── config\                    (File cấu hình)
echo   ├── database\                  (SQL scripts)
echo   ├── resources\                 (Tài nguyên bổ sung)
echo   └── docs\                      (Tài liệu)
echo.
echo ================================================================
echo   📋 HƯỚNG DẪN PHÂN PHỐI
echo ================================================================
echo.
echo 1. Giải nén file ZIP:
echo    target\SmartNotebook-1.0-SNAPSHOT-distribution.zip
echo.
echo 2. Gửi toàn bộ thư mục cho người dùng
echo.
echo 3. Người dùng làm theo hướng dẫn trong INSTALL.txt
echo.
echo 4. Chạy run.bat để khởi động ứng dụng
echo.
echo ================================================================
pause
