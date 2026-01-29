@echo off
chcp 65001 >nul
echo ================================================================
echo   📦 BUNDLE MAVEN REPOSITORY - Offline Build Support
echo ================================================================
echo.
echo Tạo gói Maven repository với tất cả dependencies để build offline
echo.

REM Tạo thư mục target nếu chưa có
if not exist "target" mkdir target
if not exist "target\maven-repo" mkdir target\maven-repo

echo [1/3] Đang download tất cả dependencies...
echo.
call mvn dependency:go-offline

if %errorlevel% neq 0 (
    echo.
    echo ❌ Lỗi khi download dependencies!
    pause
    exit /b 1
)

echo.
echo [2/3] Đang copy dependencies vào local repository...
echo.
call mvn dependency:copy-dependencies -DoutputDirectory=target\maven-repo\repository

echo.
echo [3/3] Đang tạo repository structure...
echo.

REM Copy toàn bộ dependencies từ .m2 local repository
REM Chỉ copy các dependencies của project này
set M2_REPO=%USERPROFILE%\.m2\repository

echo Copying JavaFX dependencies...
xcopy /E /I /Y "%M2_REPO%\org\openjfx" "target\maven-repo\org\openjfx\" >nul 2>&1

echo Copying SQL Server JDBC...
xcopy /E /I /Y "%M2_REPO%\com\microsoft\sqlserver" "target\maven-repo\com\microsoft\sqlserver\" >nul 2>&1

echo Copying OpenNLP...
xcopy /E /I /Y "%M2_REPO%\org\apache\opennlp" "target\maven-repo\org\apache\opennlp\" >nul 2>&1

echo Copying Maven plugins...
xcopy /E /I /Y "%M2_REPO%\org\apache\maven\plugins" "target\maven-repo\org\apache\maven\plugins\" >nul 2>&1

echo Copying Maven dependencies...
xcopy /E /I /Y "%M2_REPO%\commons-io" "target\maven-repo\commons-io\" >nul 2>&1
xcopy /E /I /Y "%M2_REPO%\org\apache\commons" "target\maven-repo\org\apache\commons\" >nul 2>&1

echo.
echo ================================================================
echo   ✅ MAVEN REPOSITORY ĐÃ ĐƯỢC BUNDLE!
echo ================================================================
echo.
echo 📂 Location: target\maven-repo\
echo.
echo 📋 Next steps:
echo   1. Chạy: build-source-distribution.bat
echo   2. File ZIP sẽ chứa cả Maven repository
echo   3. Người dùng có thể build offline với mvn-offline.bat
echo.
echo ================================================================
pause
