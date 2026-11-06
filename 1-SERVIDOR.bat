@echo off
title EcoMonitor - Servidor
REM Sobe o servidor EcoMonitor na porta 8080 (Ctrl+C para parar).
cd /d "%~dp0"
call scripts\java-env.bat || exit /b 1
if not exist target\ecomonitor.jar call "%~dp0COMPILAR.bat" nopause || exit /b 1
echo.
echo ==============================================================
echo  Servidor EcoMonitor
echo  Painel:          http://127.0.0.1:8080
echo  Usuarios:        admin / admin123     visitante / visitante123
echo  Outros PCs da rede: http://SEU_IP:8080  (veja o IPv4 abaixo)
echo ==============================================================
ipconfig | findstr /c:"IPv4"
echo.
java -jar target\ecomonitor.jar
pause
