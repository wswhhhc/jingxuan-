@echo off
echo ========================================
echo     Jingxuan Platform - Frontend Service
echo ========================================
echo.

cd /d "%~dp0frontend"

echo [1/2] Checking dependencies...
if not exist "node_modules" (
    echo node_modules not found, installing dependencies...
    call npm install
    if errorlevel 1 (
        echo [ERROR] Dependency installation failed!
        pause
        exit /b 1
    )
    echo [OK] Dependencies installed
) else (
    echo [OK] Dependencies already exist
)

echo.
echo [2/2] Starting development server...
echo.
echo Starting frontend service...
echo Access URL: http://localhost:5173
echo.
echo Press Ctrl+C to stop the service
echo ========================================
echo.

call npm run dev

pause
