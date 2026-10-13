@echo off
echo.
echo ========================================
echo   🚀 Starting FoodFast Environment...
echo ========================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Node.js is not installed or not in PATH
    echo    Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Java is not installed or not in PATH
    echo    Please install Java 17+ from https://adoptium.net/
    pause
    exit /b 1
)

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Error: Maven is not installed or not in PATH
    echo    Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Get the script directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo [1/2] Starting Spring Boot Backend on port 8080...
start "FoodFast Backend" cmd /k "cd /d %SCRIPT_DIR%backend && echo ✅ Backend starting... && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [2/2] Starting Frontend Web on port 5173...
start "FoodFast Web App" cmd /k "cd /d %SCRIPT_DIR%frontend-web && echo ✅ Web App starting... && npm install --legacy-peer-deps && npm run dev:fast"
timeout /t 3 /nobreak >nul

echo.
echo ========================================
echo   ✅ All servers launched successfully!
echo ========================================
echo.
echo 📋 Services running:
echo    • Backend API:  http://localhost:8080
echo    • Web App:      http://localhost:5173
echo    • H2 data:      backend\runtime-data\foodfast.mv.db
echo.
echo 💡 Tip: Backend and web app run in separate terminal windows.
echo    Close individual windows to stop specific services.
echo.
pause

