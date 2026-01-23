@echo off
echo ========================================
echo   KIEM TRA KET NOI SQL SERVER
echo ========================================
echo.

REM Khong compile nua, dung class da compile
echo [INFO] Su dung class da compile tu target/classes
echo [INFO] Bat TLS 1.0 trong Java de ket noi SQL Server cu
echo [INFO] Dung JDBC driver 7.4.1 (ho tro TLS 1.0)
echo.

cd target\classes
java -cp ".;C:\Users\ASUS\.m2\repository\com\microsoft\sqlserver\mssql-jdbc\7.4.1.jre8\mssql-jdbc-7.4.1.jre8.jar" ^
  -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2 ^
  -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2 ^
  com.dat.notebook.util.DBConnection

cd ..\..
echo.
echo ========================================
pause
