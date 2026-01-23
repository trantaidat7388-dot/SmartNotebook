@echo off
echo ========================================
echo   CẬP NHẬT PASSWORD HASH CHO USER
echo ========================================
echo.
echo Script này sẽ hash password trong database
echo sử dụng MD5 để khớp với Java PasswordUtil
echo.
echo Nhấn Enter để tiếp tục hoặc Ctrl+C để hủy
pause

sqlcmd -S LAPTOP-AJ5RKUT\SQL,1435 -U trantandatSQL -P 221761 -d SmartNotebookDB -i UpdatePasswordHash.sql

echo.
echo ========================================
echo Đã cập nhật password hash!
echo.
echo Bây giờ có thể đăng nhập:
echo   - Username: admin, Password: admin123
echo   - Username: dat, Password: dat123
echo ========================================
pause
