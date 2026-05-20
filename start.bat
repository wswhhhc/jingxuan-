@echo off

set ROOT_DIR=%~dp0
set BACKEND_DIR=%ROOT_DIR%backend
set FRONTEND_DIR=%ROOT_DIR%frontend

echo Killing old processes...
netstat -ano | findstr ":8080 " | findstr LISTENING > port.txt
if exist port.txt for /f "tokens=5" %%a in (port.txt) do taskkill /F /PID %%a
netstat -ano | findstr ":5173 " | findstr LISTENING > port.txt
if exist port.txt for /f "tokens=5" %%a in (port.txt) do taskkill /F /PID %%a
del port.txt 2>nul
timeout /t 2 >nul

echo Building backend...
cd /d "%BACKEND_DIR%"
call D:\java\Maven\apache-maven-3.9.10\bin\mvn package -DskipTests -Dmaven.clean.skip=true -q
if errorlevel 1 (
    echo Build failed
    pause
    exit /b 1
)

echo Starting backend...
for /f "tokens=2 delims==" %%a in ('type "%ROOT_DIR%.env" 2^>nul ^| findstr /b "DEEPSEEK_API_KEY"') do set DEEPSEEK_API_KEY=%%a
start "Jingxuan-Backend" D:\java\jdk\bin\java -jar "%BACKEND_DIR%\target\jingxuan-backend-1.0.0.jar" --spring.profiles.active=dev

echo Waiting...
timeout /t 15 >nul

echo Starting frontend...
cd /d "%FRONTEND_DIR%"
start "Jingxuan-Frontend" "%FRONTEND_DIR%\node_modules\.bin\vite.cmd" --port 5173
timeout /t 3 >nul

echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
pause
