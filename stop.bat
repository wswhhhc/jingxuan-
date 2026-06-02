@echo off
echo ========================================
echo   Stop Jingxuan Services
echo ========================================
echo.

powershell -NoProfile -Command "$c=0; netstat -ano | Select-String ':(8080|5173) ' | ForEach-Object { $i = $_ -replace '.*\s+(\d+)\s*$','$1'; if($i -ne 0){taskkill /f /pid $i 2>$null; write-host ('[OK] Stopped PID ' + $i); $c++} }; if($c -eq 0){write-host '[INFO] Ports 8080 and 5173 are free.'}else{write-host ('[OK] ' + $c + ' process(es) stopped.')}"

echo.
pause >nul
