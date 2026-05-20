@echo off
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p=8080,5173;foreach($i in $p){try{$c=Get-NetTCPConnection -LocalPort $i -ErrorAction Stop;Stop-Process $c.OwningProcess -Force;Write-Host ('Killed port '+$i)}catch{Write-Host ('Port '+$i+': free')}};Start-Sleep 2"
pause
