@echo off
chcp 65001 >nul
echo ================================================
echo   🚀 SMART NOTEBOOK - AUTO SETUP
echo   Tự động cài đặt môi trường và dependencies
echo ================================================
echo.

REM Check Java
echo [1/5] Kiểm tra Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ JAVA chưa cài đặt!
    echo 📥 Download tại: https://www.oracle.com/java/technologies/downloads/#java17
    pause
    exit /b 1
)
echo ✅ Java đã cài đặt

REM Check if mvnw exists, if not use Maven Wrapper
echo.
echo [2/5] Cài đặt Maven Wrapper...
if not exist "mvnw.cmd" (
    echo Downloading Maven Wrapper...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile 'maven-wrapper.jar'}"
    echo @REM Maven Wrapper > mvnw.cmd
    echo @echo off >> mvnw.cmd
    echo set MAVEN_PROJECTBASEDIR=%%~dp0 >> mvnw.cmd
    echo mvn %%* >> mvnw.cmd
)
echo ✅ Maven Wrapper sẵn sàng

REM Clean install
echo.
echo [3/5] Download dependencies (có thể mất 3-5 phút)...
echo Đang tải JavaFX, SQL Server Driver, và các thư viện...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo ❌ Lỗi download dependencies
    pause
    exit /b 1
)
echo ✅ Dependencies đã download

REM Check SQL Server
echo.
echo [4/5] Kiểm tra SQL Server...
echo ⚠️  Lưu ý: Đảm bảo SQL Server đang chạy
echo.
echo Nếu chưa có database 'SmartNotebook', script sẽ tự tạo...
timeout /t 3 >nul

REM Try to create database (optional, may fail if already exists)
sqlcmd -S localhost -U trantandatSQL -P 123456 -Q "IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'SmartNotebook') CREATE DATABASE SmartNotebook" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Database 'SmartNotebook' đã sẵn sàng
) else (
    echo ⚠️  Không thể auto-create database
    echo ℹ️  Vui lòng tạo database thủ công hoặc kiểm tra SQL Server
)

REM Compile
echo.
echo [5/5] Biên dịch project...
call mvn compile
if %errorlevel% neq 0 (
    echo ❌ Lỗi biên dịch
    pause
    exit /b 1
)
echo ✅ Biên dịch thành công

echo.
echo ================================================
echo   ✅ SETUP HOÀN TẤT!
echo ================================================
echo.
echo 📝 Bước tiếp theo:
echo   1. Kiểm tra file 'db.properties' (nếu cần sửa SQL)
echo   2. Chạy ứng dụng: double-click 'run.bat'
echo.
echo ⚙️  Nếu SQL Server khác cổng 1433:
echo    → Sửa 'db.properties'
echo.
pause
