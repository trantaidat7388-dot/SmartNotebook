@echo off
echo ============================================
echo SmartNotebook - Rich Text Editor Setup
echo ============================================
echo.
echo Chay SQL Schema cho Rich Text Editor...
echo.

REM Thay doi cac thong tin ket noi theo database cua ban
set SERVER=localhost
set DATABASE=SmartNotebook_DB
set USERNAME=sa
set PASSWORD=yourpassword

REM Chay SQL script
sqlcmd -S %SERVER% -d %DATABASE% -U %USERNAME% -P %PASSWORD% -i RichTextNotes_Schema.sql

if %ERRORLEVEL% == 0 (
    echo.
    echo ============================================
    echo Schema setup thanh cong!
    echo ============================================
) else (
    echo.
    echo ============================================
    echo Co loi xay ra khi chay schema!
    echo Vui long kiem tra ket noi database.
    echo ============================================
)

echo.
pause
