@echo off
chcp 65001 >nul
title Payanam Travel Platform - Java Web Server
cls
echo ===================================================================
echo               PAYANAM - Tourist Booking Web Platform
echo ===================================================================
echo.
echo Java Version Check:
java -version
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [1/2] Compiling Payanam Java Source Code...
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src/main/java src/main/java/com/payanam/model/*.java src/main/java/com/payanam/util/*.java src/main/java/com/payanam/repository/*.java src/main/java/com/payanam/server/*.java src/main/java/com/payanam/Main.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. Please check errors above.
    pause
    exit /b %ERRORLEVEL%
)

echo [OK] Compilation successful!
echo.
echo [2/2] Starting Payanam Web Server at http://localhost:8085 ...
echo.
echo -------------------------------------------------------------------
echo  * Open Web Browser:  http://localhost:8085
echo  * Default Admin:     admin@gmail.com / admin123
echo  * Default Tourist:   rahul@gmail.com / rahul123
echo -------------------------------------------------------------------
echo Press Ctrl+C in this console window to stop the server anytime.
echo.

start "" "http://localhost:8085"
java -cp bin com.payanam.Main 8085
pause
